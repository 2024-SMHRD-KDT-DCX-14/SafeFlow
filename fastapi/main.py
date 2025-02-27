import sys
import os
import torch
import base64
import json
import cv2
import numpy as np
import asyncio
from pathlib import Path
from datetime import datetime
from collections import deque
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from starlette.middleware.cors import CORSMiddleware
import aiohttp
from aiohttp import MultipartWriter

# ✅ pathlib.PosixPath 문제 해결
import pathlib
pathlib.PosixPath = pathlib.WindowsPath  # Windows에서 충돌 방지

# ✅ FastAPI 초기화
app = FastAPI()

# ✅ CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 모든 도메인 허용
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ✅ YOLOv5 모델 로드 (🔥 torch.hub.load() 방식)
MODEL_PATH = str(Path("C:/Users/dony6/Desktop/SafeFlow-master/SafeFlow-master/fastapi/yolov5/best2.pt"))
YOLOV5_REPO = str(Path("C:/Users/dony6/Desktop/SafeFlow-master/SafeFlow-master/fastapi/yolov5"))

model = torch.hub.load(
    YOLOV5_REPO,   # YOLOv5 로컬 경로
    'custom',      # 사용자 정의 YOLO 모델
    path=MODEL_PATH,  # 학습된 모델 경로
    source='local',  # 로컬 YOLOv5 사용
    force_reload=True  # 캐시 무시하고 강제 로드
)

print("✅ YOLOv5 모델 로드 완료!")

# ✅ Java API URL (CCTV 이벤트 전송)
SPRING_BOOT_API_URL = "http://127.0.0.1:8081/cctv/event"

# ✅ WebSocket 클라이언트 관리
clients = set()
frame_history = {}  # 각 cctv_idx별 프레임 저장
BUFFER_SIZE = 15  # ✅ 최신 15개 프레임 저장
COOLDOWN_TIME = 30  # 30초 동안 추가 전송 방지
last_event_time = {}

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    """ WebSocket 연결 및 YOLO 프레임 처리 """
    await websocket.accept()
    clients.add(websocket)
    print(f"✅ WebSocket 연결됨: {websocket.client}")

    try:
        while True:
            data = await websocket.receive_text()
            frame_data = json.loads(data)

            cctv_idx = frame_data.get("cctv_idx", None)
            if cctv_idx is None:
                print("❌ cctv_idx 없음. YOLO 분석 진행 불가.")
                continue

            frame_bytes = base64.b64decode(frame_data["image"])
            np_arr = np.frombuffer(frame_bytes, np.uint8)
            img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

            if img is None:
                continue

            # 프레임 버퍼 초기화
            if cctv_idx not in frame_history:
                frame_history[cctv_idx] = deque(maxlen=BUFFER_SIZE)

            frame_history[cctv_idx].append(img)

            # YOLO 탐지 수행
            detections, img_with_boxes = perform_yolo_detection(img, cctv_idx)

            print(f"✅ 감지된 객체 개수: {len(detections)}개, 객체 목록: {[det['class'] for det in detections]}")

            await send_yolo_data_to_clients(cctv_idx, detections)

    except WebSocketDisconnect:
        clients.discard(websocket)
    except Exception as e:
        print(f"WebSocket 오류 발생: {e}")
        clients.discard(websocket)


def perform_yolo_detection(img, cctv_idx):
    """ YOLO 감지 수행 및 바운딩 박스가 포함된 이미지 반환 """
    results = model(img)  # ✅ YOLO 감지 실행

    detections = []
    event_detected = False  # 🚨 이벤트 발생 여부 플래그
    event_type = None  # 🔥 이벤트 타입 (head 또는 helmet)
    
    CONFIDENCE_THRESHOLD = 0.80  # ✅ 컨피던스 80% 이상만 포함

    for *xyxy, conf, cls in results.xyxy[0].tolist():
        if conf < CONFIDENCE_THRESHOLD:
            continue  # ✅ 80% 미만 객체 필터링

        x1, y1, x2, y2 = map(int, xyxy)
        class_name = model.names[int(cls)]

        detections.append({
            "class": class_name,
            "bbox": [x1, y1, x2, y2],
            "confidence": float(conf)
        })

        # ✅ 특정 객체 감지 시 이벤트 발생 처리
        if class_name in {"fire", "head"}:  
            print(f"🚨 [EVENT DETECTED] {class_name} 감지됨! (CCTV: {cctv_idx})")  
            event_detected = True
            event_type = class_name

        # ✅ 바운딩 박스 추가
        cv2.rectangle(img, (x1, y1), (x2, y2), (0, 255, 0), 2)
        cv2.putText(img, f"{class_name} ({conf:.2f})", (x1, y1 - 10),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)

    # ✅ 이벤트 감지 시 Java API로 전송
    if event_detected:
        if cctv_idx not in last_event_time or (datetime.now() - last_event_time[cctv_idx]).seconds > COOLDOWN_TIME:
            last_event_time[cctv_idx] = datetime.now()
            print(f"📡 [SENDING TO JAVA] 이벤트 {event_type} 전송 (CCTV {cctv_idx})")  
            asyncio.create_task(send_event_to_java(cctv_idx, event_type, detections, frame_history.get(cctv_idx, [])))

    return detections, img


async def send_yolo_data_to_clients(cctv_idx, detections):
    """ 감지된 YOLO 데이터를 WebSocket을 통해 클라이언트에게 전송 """
    message = json.dumps({"cctv_idx": cctv_idx, "detections": detections}, indent=4)
    for client in clients:
        try:
            await client.send_text(message)
        except Exception:
            clients.remove(client)

            
async def send_event_to_java(cctv_idx, event_type, detections, frames):
    """ 특정 객체 감지 시, 바운딩 박스 포함된 프레임을 Java로 전송 """

    detection_time = datetime.now().strftime("%Y%m%d_%H%M%S")
    print(f"📡 [DEBUG] Java 서버로 이벤트 전송 준비: CCTV {cctv_idx}, 이벤트 {event_type}")

    # ✅ 탐지 직후 5개의 미래 프레임이 쌓일 때까지 대기 (5초 동안 추가적인 프레임 수집)
    await asyncio.sleep(5)  # 🚀 미래 프레임을 확보하기 위해 5초 대기
    print(f"📡 [DEBUG] 5초 대기 완료, 프레임 최종 개수 (CCTV {cctv_idx}): {len(frames)}")

    # ✅ 최신 15개 프레임 중에서 과거 4 + 현재 1 + 미래 5 선택
    selected_frames = []
    if frames and len(frames) >= 10:
        event_index = len(frames) - 6  # 현재 프레임 기준 인덱스 (-6부터 시작)
        selected_frames = list(frames)[max(event_index-4, 0): event_index+6]  # 과거 4 + 현재 1 + 미래 5
    else:
        print(f"⚠ [DEBUG] 충분한 미래 프레임이 없음 (CCTV {cctv_idx})")

    async with aiohttp.ClientSession() as session:
        mpwriter = MultipartWriter("form-data")  

        # ✅ 기본 데이터 필드 추가
        part = mpwriter.append(f"{cctv_idx}")
        part.set_content_disposition("form-data", name="cctv_idx")

        part = mpwriter.append(event_type)
        part.set_content_disposition("form-data", name="eventType")

        part = mpwriter.append(detection_time)
        part.set_content_disposition("form-data", name="detectionTime")

        if selected_frames:
            print(f"📸 [DEBUG] Java로 전송할 프레임 개수: {len(selected_frames)}")

            for i, frame in enumerate(selected_frames):
                success, buffer = cv2.imencode(".jpg", frame)
                if not success:
                    print(f"⚠ [ERROR] 프레임 {i+1} 인코딩 실패 (CCTV {cctv_idx})")
                    continue

                part = mpwriter.append(buffer.tobytes())
                part.set_content_disposition("form-data", name="frames", filename=f"frame{i+1}.jpg")
                part.headers["Content-Type"] = "image/jpeg"

        else:
            print(f"⚠ [DEBUG] 전송할 프레임이 없음 (CCTV {cctv_idx})")

        headers = {"Content-Type": f"multipart/form-data; boundary={mpwriter.boundary}"}

        async with session.post(SPRING_BOOT_API_URL, data=mpwriter, headers=headers) as response:
            response_text = await response.text()
            if response.status == 200:
                print(f"✅ Java API 전송 성공! 응답: {response_text}")
            else:
                print(f"⚠ Java API 응답 오류 ({response.status}): {response_text}")