# Safeflow

---

## 📌 프로젝트 소개
Safe Flow 프로젝트는 공장 및 산업 현장의 안전 관리를 위한 이상 탐지 및 문서 검색 챗봇 서비스입니다.
YOLO v5 기반 객체 탐지 기술을 활용해 안전모 착용 여부 및 화재 발생을 실시간 감지하며, RAG 챗봇 기능을 통해 안전 규정, 작업 절차, 품질 기준 등을 빠르게 검색할 수 있어 관리자 업무를 효율화합니다.
이를 통해 산업재해를 사전에 방지하고, 안전 관리자의 업무 부담을 줄이며, 보다 안전한 작업 환경을 조성하는 것이 목표입니다.


### 🎯 프로젝트 개요
- **아이디어 주제**: 산업 현장의 이상 탐지 및 문서 검색 기능을 갖춘 안전 관리 챗봇 서비스
- **개발 목표**:
  - 안전 이상 탐지 시스템 개발: YOLO 기반 객체 탐지를 활용하여 안전모 착용 
     여부, 화재 발생 등을 실시간 감지하고 경고
  - RAG 기반 안전 규정 검색 챗봇 개발: 작업 절차, 품질 기준, 법규 정보를 빠르게 검색 및 제공
  - 안전 관리 효율화: 위험 요소 발생 시 신속한 대응을 지원하고, 보고서 자동 생성 기능 추가
- **개발 배경**:
  - 산업 현장의 안전사고 증가: 국내 제조업 및 공장에서 발생하는 산업재해 예방이 중요한 문제로 대두
  - 법규 준수 요구 증가: **중대재해처벌법 확대 적용(50인 → 5인 이상 사업장)**으로 안전 관리 강화 필요
  - 기존 시스템의 한계: 위험 감지 및 법규 검색이 비효율적이며, 수작업 중심의 안전 관리로 실시간 대응 어려움

---

## 🖼️ 로고 및 이미지
<p align="center">
  <img src="https://github.com/user-attachments/assets/752646f7-0345-4b57-9081-bbaf5b3d0cba" width="300">


---

## 🛠 기술 스택
| 분야         | 기술 스택 |
|-------------|------------------------------------------------|
| **Frontend** | JavaScript, HTML, CSS, Bootstrap |
| **Backend** | Spring Boot(JAVA 서버), FastAPI(실시간 객체탐지)|
| **Database** | OracleDB, ChromaDB(문서 검색 및 안전 데이터 관리) |
| **AI 모델링** | YOLOv5, OpenCV(실시간 객체 탐지 및 추적) |


---

## 📑 주요 화면 구성
- **대시보드 화면**: 감지된 이벤트(이상탐지) 및 문서 관련 대시보드
  ![image](https://github.com/user-attachments/assets/d6c987eb-3de1-4cdf-832b-379efd49478c)
- **CCTV 화면**: 작업장 내 실시간 상황 확인
![image](https://github.com/user-attachments/assets/c3a351ce-e022-4122-a7db-dfb69cb5f2bd)
- **RAG(검색증강 챗봇) **: 등록된 문서 기반 작업장 안전관리 매뉴얼 관련 답변 제
![image](https://github.com/user-attachments/assets/8d37a609-1f87-457b-9640-c64258b83418)



---

## 🌟 핵심 기능
✔ **이상 탐지 및 실시간 경고 시스템**  
&nbsp;&nbsp;&nbsp;&nbsp;🔹 YOLOv5 기반 객체 탐지를 활용하여 안전모 미착용, 화재 발생(연기·불꽃) 등을 실시간 감지  
&nbsp;&nbsp;&nbsp;&nbsp;🔹 이상 발생 시 알림 팝업 효과 적용 

✔ **RAG 기반 안전 규정 검색 챗봇**  
&nbsp;&nbsp;&nbsp;&nbsp;🔹 신호 위반 및 속도 위반 차량 자동 감지  
&nbsp;&nbsp;&nbsp;&nbsp;🔹 자연어 질문 입력 시 관련 안전 규정 및 대응 절차를 제공 

✔ **일탈 현황 및 보고서 자동 생성**  
&nbsp;&nbsp;&nbsp;&nbsp;🔹 미고고 일탈 현황(처리 일탈/총 일탈) 분석 및 대시보드 파이차트 시각화

✔ ** 대시보드 & CCTV 데이터 관리**  
&nbsp;&nbsp;&nbsp;&nbsp;🔹 월 단위 사고 발생 횟수 차트 제공 
&nbsp;&nbsp;&nbsp;&nbsp;🔹 이상 탐지 시 자동 저장된 CCTV 영상을 조회 및 재확인 가능 

---

## 🔧 프로젝트 구성도

### 1️⃣ 소프트웨어 구성도
![image](https://github.com/user-attachments/assets/1525723e-bf13-4a03-b185-ea1f3479bafc)


### 2️⃣ 서비스 흐름도: 신호 체계 로직
![image18](https://github.com/user-attachments/assets/fa89aa06-c2bb-4f83-83a2-5f98c3bc02b9)



---
