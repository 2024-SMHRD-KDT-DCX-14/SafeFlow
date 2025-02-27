const TARGET_WIDTH = 640;
const TARGET_HEIGHT = 480;

const EVENT_COOLDOWN_TIME = 30000; // 30초 (ms)
const lastEventTime = {}; // 각 cctvIdx별 마지막 이벤트 발생 시간 저장

//  전역 변수
window.cameraStreams = [];
window.cameraCodes = [];
window.cctvMapping = {};  // 🔹 slot 번호 → 실제 `cctvIdx` 매핑 저장


async function initializeCCTV() {
    // 먼저 매핑 정보를 가져옴
    await fetchCCTVMapping();

    // 등록된 CCTV가 없으면 자동 등록 시도
    if (Object.keys(window.cctvMapping).length === 0) {
        console.log("등록된 CCTV가 없으므로 자동 등록을 시도합니다.");
        
        // 예시: 사용 가능한 카메라의 deviceId를 cameraCodes로 사용
        const devices = await navigator.mediaDevices.enumerateDevices();
        const videoDevices = devices.filter(device => device.kind === "videoinput");
        const cameraCodes = videoDevices.map(device => device.deviceId);
        
        // 전역 변수에 저장 (필요하다면)
        window.cameraCodes = cameraCodes;
        
        // CCTV 등록 호출
        await registerCCTV(cameraCodes);
        
        // 등록 후 다시 매핑 정보를 갱신
        await fetchCCTVMapping();
    }
}

document.addEventListener("DOMContentLoaded", async () => {
    await fetchSessionInfo();
    await initializeCCTV();
    startWebSocket(() => {
        startStreaming();
    });
});


async function fetchCCTVMapping() {
	try {

		const response = await fetch("http://localhost:8081/cctv/list", {
			method: "GET",
			headers: { "Accept": "application/json" }
		});

		if (!response.ok) {
			console.error("❌ 오류 메시지:", await response.text());
			return;
		}

		const cctvList = await response.json();

		//  매핑 초기화 및 `undefined` 방지
		if (!Array.isArray(cctvList) || cctvList.length === 0) {
			return;
		}

		window.cctvMapping = {};
		cctvList.forEach((cctv, index) => {
			if (cctv.cctvIdx !== undefined && cctv.cctvIdx !== null) {
				window.cctvMapping[index] = {
					cctvIdx: cctv.cctvIdx,
					cctvLoc: cctv.cctvLoc  // 🔹 CCTV 위치 정보 추가
				};
			}
		});

	} catch (error) {
		console.error("❌ 서버 통신 오류 (CCTV 리스트 요청 실패):", error);
	}
}

//  서버에서 세션 정보 가져오기
async function fetchSessionInfo() {
	try {
		const response = await fetch("/auth/session");
		if (response.ok) {
			const data = await response.json();

			if (data.empNo && data.empDept) {
				sessionStorage.setItem("EMP_NO", data.empNo);
				sessionStorage.setItem("EMP_DEPT", data.empDept);
			}
		}
	} catch (error) {
		console.error("❌ 서버 통신 오류:", error);
	}
}

//  카메라 스트림을 가져와 WebSocket으로 전송
async function startStreaming() {
	try {
		const devices = await navigator.mediaDevices.enumerateDevices();
		const videoDevices = devices.filter(device => device.kind === "videoinput");
		if (videoDevices.length < 1) {
			console.warn("카메라가 필요합니다.");
			return;
		}


		//  기존 스트림 유지 (트랙을 중지하지 않고, 필요할 때만 새로 요청)
		for (let i = 0; i < Math.min(videoDevices.length, 4); i++) {
			let stream = window.cameraStreams[i];

			if (!stream || stream.getTracks().every(track => track.readyState === "ended")) {
				stream = await navigator.mediaDevices.getUserMedia({
					video: { deviceId: videoDevices[i].deviceId }
				});

				window.cameraStreams[i] = stream;
			}
		}

		console.log(`📸 ${window.cameraStreams.length}개의 카메라가 백그라운드에서 활성화됨.`);

		setInterval(() => {
			if (!window.webSocket || window.webSocket.readyState !== WebSocket.OPEN) {
				console.warn("⚠️ WebSocket이 닫혀 있음. 재연결 시도 중...");
				startWebSocket();
				return;
			}

			window.cameraStreams.forEach((stream, index) => {
				const videoTrack = stream.getVideoTracks()[0];
				if (!videoTrack || videoTrack.readyState !== "live") {
					console.warn(`⚠️ 사용 불가능한 트랙 발견 (slot-${index}), 스트림 갱신 필요`);
					return;
				}

				const imageCapture = new ImageCapture(videoTrack);
				imageCapture.grabFrame().then(imageBitmap => {

					//  캔버스 640x480 크기로 설정 (WebSocket 전송용)
					const canvas = document.createElement("canvas");
					canvas.width = TARGET_WIDTH;
					canvas.height = TARGET_HEIGHT;
					const ctx = canvas.getContext("2d");
					ctx.drawImage(imageBitmap, 0, 0, TARGET_WIDTH, TARGET_HEIGHT);

					//  WebSocket으로 640x480 프레임 전송
					canvas.toBlob(blob => {
						const reader = new FileReader();
						reader.readAsDataURL(blob);
						reader.onloadend = () => {
							const base64data = reader.result.split(",")[1];

							const cctvIdx = window.cctvMapping[index].cctvIdx || null;
							if (!cctvIdx) {
								return;
							}

							const payload = {
								slot_number: index,
								cctv_idx: cctvIdx, image: base64data
							};
							window.webSocket.send(JSON.stringify(payload));
						};
					}, "image/jpeg");
				}).catch(error => console.error("❌ 프레임 캡처 오류:", error));
			});
		}, 1000);


	} catch (error) {
		console.error("❌ 웹캠 접근 오류:", error);
	}
}

//  카메라 정보를 서버(Spring Boot)에 등록
async function registerCCTV(cameraCodes) {
	let empNo = sessionStorage.getItem("EMP_NO");
	let empDept = sessionStorage.getItem("EMP_DEPT");

	if (!empNo || !empDept) {
		console.error("❌ 세션 정보 없음");
		await fetchSessionInfo();
		empNo = sessionStorage.getItem("EMP_NO");
		empDept = sessionStorage.getItem("EMP_DEPT");

		if (!empNo || !empDept) {
			console.error("❌ 세션 정보 없음");
			return;
		}
	}

	const requestBody = {
		empNo: empNo,
		empDept: empDept,
		cctvCodes: [...cameraCodes]
	};

	try {
		const response = await fetch("/cctv/register", {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify(requestBody)
		});

		if (response.ok) {
			console.log(" CCTV 등록 완료:", await response.json());
		} else {
			console.error("❌ CCTV 등록 실패:", await response.text());
		}
	} catch (error) {
		console.error("❌ 서버 전송 오류:", error);
	}
}

//  CCTV 화면을 content에 삽입
document.querySelector("cctv").addEventListener("click", async () => {
	try {
		let response = await fetch("/cctv");
		let html = await response.text();

		let contentDiv = document.querySelector("content");
		contentDiv.innerHTML = html;

		setTimeout(() => {
			loadCCTVStreams();
		}, 500);
	} catch (error) {
		console.error("❌ CCTV 페이지 로드 실패:", error);
	}
});


//  cctv.html이 로드될 때 기존 스트림을 비디오 슬롯에 자동 매칭
function loadCCTVStreams() {
	const videoElements = document.querySelectorAll(".grid-video");

	if (window.cameraStreams.length === 0) {
		console.warn("❌ 사용할 수 있는 카메라가 없습니다.");
		return;
	}
	// 모든 기존 비디오 슬롯에 스트림 초기화 (정환추가)
	videoElements.forEach((video, index) => {
		if (video.srcObject) {
			const oldStream = video.srcObject;
			oldStream.getTracks().forEach(track => track.stop()); // 모든 트랙 중지
			video.srcObject = null;
		}
	});

	//  초기 실행 시 slot-1을 기본 활성화(정환추가)
	const gridSlots = document.querySelectorAll(".grid-slot");
	let activeSlot = document.querySelector(".grid-slot.active");

	if (!activeSlot) {
		gridSlots[0].classList.add("active"); // slot-0을 기본 활성화
		activeSlot = gridSlots[0];
	}

	const slotIndex = [...gridSlots].indexOf(activeSlot);

	//  기본 활성화된 slot-0에 첫 번째 스트림 연결(정환추가)
	if (slotIndex >= 0 && slotIndex < window.cameraStreams.length) {
		videoElements[slotIndex].srcObject = window.cameraStreams[0]; // 첫 번째 카메라 스트림 할당
	}

	window.cameraStreams.forEach((stream, index) => {
		if (index < videoElements.length) { //바꿀부분
			if (!videoElements[index]) {
				return;
			}
			videoElements[index].srcObject = stream;
		}
	});
}


function startWebSocket(callback) {
	window.webSocket = new WebSocket("ws://localhost:8002/ws");

	window.webSocket.onopen = () => {
		console.log(" WebSocket 연결");
		if (typeof callback === "function") {
			callback();  //  WebSocket 연결 후 startStreaming 실행
		}
	};

	window.webSocket.onmessage = (event) => {
		try {
			const data = JSON.parse(event.data);
			console.log("📡 YOLO 감지 데이터 수신:", data);

			if (!data.detections || !Array.isArray(data.detections)) {
				return;
			}

			const { cctv_idx, detections } = data;


			const detectedObjects = detections.map(d => d.class);

			// ✅ fire 또는 cup 감지 시 Cooldown 체크 후 모달 띄우기
			if (detectedObjects.includes("head") || detectedObjects.includes("helmet")) {
				const currentTime = Date.now();
				const lastTime = lastEventTime[cctv_idx] || 0;

				if (currentTime - lastTime > EVENT_COOLDOWN_TIME) {
					lastEventTime[cctv_idx] = currentTime; // ✅ 최신 이벤트 시간 저장
					showEventModal(cctv_idx, detectedObjects);
				}
			}

			//  데이터 검증: bbox에 4개의 값이 있는지 확인
			for (const detection of detections) {
				if (!detection.bbox || detection.bbox.length !== 4) {
					return;
				}
			}

			//  `cctv_idx`를 이용하여 `slot_number` 찾기
			let slot_number = null;
			for (const [slot, cctvInfo] of Object.entries(window.cctvMapping)) {
				if (cctvInfo.cctvIdx == cctv_idx) {
					slot_number = slot;
					break;
				}
			}


			if (slot_number === null) {
				return;
			}

			// 감지 데이터가 있는 경우 바운딩 박스 업데이트(정환 수정)
			if (detections.length > 0) {
				drawDetections(slot_number, detections);
			}
		} catch (error) {
			console.error("❌ YOLO 데이터 파싱 오류:", error);
		}
	};


	window.webSocket.onerror = (error) => {
	};

	window.webSocket.onclose = () => {
	};
}

function drawDetections(slot_number, detections) {
	const videoElement = document.querySelector(`#video-${slot_number}`);
	const canvasElement = document.querySelector(`#canvas-${slot_number}`);

	if (!videoElement || !canvasElement) {
		return;
	}

	const ctx = canvasElement.getContext("2d");

	// 🌟 캔버스 크기 가져오기
	const canvasWidth = canvasElement.width;
	const canvasHeight = canvasElement.height;

	// 🌟 비디오 원본 크기 가져오기
	const videoWidth = videoElement.videoWidth;
	const videoHeight = videoElement.videoHeight;

	// 📌 스케일 비율 계산 (YOLO가 원본 크기로 감지했을 경우 대비)
	const scaleX = canvasWidth / videoWidth;
	const scaleY = canvasHeight / videoHeight;

	// ✅ 기존 감지 박스 제거
	ctx.clearRect(0, 0, canvasWidth, canvasHeight);

	detections.forEach((detection) => {
		const { class: detectedClass, bbox, confidence } = detection;
		let [x1, y1, x2, y2] = bbox;

		// 📌 바운딩 박스를 캔버스 크기에 맞게 조정
		x1 *= scaleX;
		y1 *= scaleY;
		x2 *= scaleX;
		y2 *= scaleY;

		// 🎨 바운딩 박스 그리기
		ctx.strokeStyle = "red";
		ctx.lineWidth = 2;
		ctx.strokeRect(x1, y1, x2 - x1, y2 - y1);

		// 🎨 클래스명과 신뢰도 표시
		ctx.fillStyle = "red";
		ctx.font = "14px Arial";
		ctx.fillText(`${detectedClass} (${(confidence * 100).toFixed(1)}%)`, x1, y1 - 5);
	});
}

// ✅ 모달 상태 추적 변수
let isModalOpen = false;  // 중복 방지
let pendingEvents = [];  // 초기 이벤트 저장
window.eventDetectionReady = false;  // 초기 감지 무시

// ✅ 페이지 로딩 후 2초 후 이벤트 감지 활성화
setTimeout(() => {
    window.eventDetectionReady = true;

    // 🔥 로딩 중 무시된 이벤트 처리
    pendingEvents.forEach(event => showEventModal(event.cctvIdx, event.detectedObjects));
    pendingEvents = [];
}, 2000);

// ✅ 모달 HTML을 미리 추가 (초기엔 숨김)
document.body.insertAdjacentHTML("beforeend", `
    <div id="eventModal" class="modal-overlay" style="display: none; opacity: 0; transition: opacity 0.5s ease-in-out;">
        <div class="modal-content">
            <h2 id="modalTitle">🚨 이벤트 발생</h2>
            <div class="modal-body">
                <div class="modal-video">
                    <video id="modalVideo" autoplay></video>
                </div>
                <div class="modal-info">
                    <span id="modalTime"><b>발생 시간:</b></span><br>
                    <span id="modalMessage"><b>탐지 객체:</b></span>
                </div>
            </div>
            <button id="closeModalBtn">닫기</button>
        </div>
    </div>
`);

// ✅ 모달 닫기 버튼 이벤트 추가
document.getElementById("closeModalBtn").addEventListener("click", closeModal);

function showEventModal(cctvIdx, detectedObjects) {
    // ✅ 초기에 감지된 객체는 저장만 해두고 무시
    if (!window.eventDetectionReady) {
        pendingEvents.push({ cctvIdx, detectedObjects });
        return;
    }

    // ✅ 모달 중복 실행 방지
    if (isModalOpen) {
        return;
    }
    isModalOpen = true;

    // 🔹 cctvIdx에 해당하는 cctvLoc 찾기
    let cctvLoc = "알 수 없음";
    for (const [slot, cctvInfo] of Object.entries(window.cctvMapping)) {
        if (cctvInfo.cctvIdx == cctvIdx) {
            cctvLoc = cctvInfo.cctvLoc || "알 수 없음";
            break;
        }
    }

    // ✅ 탐지 객체 메시지 생성
    let detectionMessage = "<b>탐지 객체:</b> ";
    if (detectedObjects.includes("fire")) {
        detectionMessage += "<span style='color: red;'>🔥 화재 발생</span>";
    } else if (detectedObjects.includes("helmet")) {
        detectionMessage += "<span style='color: orange;'>⚠️ 헬멧 미착용자 확인</span>";
    } else {
        detectionMessage += "<span>🚨 특이사항 없음</span>";
    }

    // ✅ 기존 모달 HTML 업데이트 (새로운 HTML 추가 X)
    document.getElementById("modalTitle").innerText = `🚨 이벤트 발생: ${cctvLoc}`;
    document.getElementById("modalTime").innerHTML = `<b>발생 시간:</b> ${new Date().toLocaleString()}`;
    document.getElementById("modalMessage").innerHTML = detectionMessage;

    // ✅ 모달 표시 (display: none → flex 후 opacity 1)
    const modal = document.getElementById("eventModal");
    modal.style.display = "flex";
    setTimeout(() => {
        modal.style.opacity = "1";
    }, 50);

    // ✅ 비디오 스트림 연결
    const slot_number = Object.keys(window.cctvMapping).find(
        slot => window.cctvMapping[slot]?.cctvIdx == cctvIdx
    );

    const videoElement = document.getElementById("modalVideo");
    if (slot_number !== undefined && window.cameraStreams[slot_number]) {
        videoElement.srcObject = window.cameraStreams[slot_number];
    } else {
    }
}

// ✅ 모달 닫기 함수
function closeModal() {
    const modal = document.getElementById("eventModal");
    if (modal) {
        modal.style.opacity = "0";  // 서서히 사라지게 설정
        setTimeout(() => {
            modal.style.display = "none";
            isModalOpen = false;  // ✅ 모달 닫기 후 다시 열 수 있도록 상태 초기화
        }, 500);
    }
}
