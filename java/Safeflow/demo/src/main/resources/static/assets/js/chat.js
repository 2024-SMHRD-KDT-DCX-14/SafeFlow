// DOM Elements
const chatToggleButton = document.getElementById("chatToggleButton");
const chatContainer = document.getElementById("chatContainer");
const closeChatBtn = document.getElementById("closeChatBtn");
const chatBox = document.getElementById("chatBox");
const chatInput = document.querySelector(".chatInput");
const chatInputField = document.getElementById("chatInputField");
const sendMessageBtn = document.getElementById("sendMessageBtn");
const backBtn = document.getElementById("backBtn");

// 채팅창 열기/닫기
function setupEventListeners() {
	chatToggleButton.addEventListener("click", toggleChatContainer);
	closeChatBtn.addEventListener("click", closeChatContainer);
	chatInputField.addEventListener("keypress", handleEnterKey);
	sendMessageBtn.addEventListener("click", sendMessage);
	backBtn.addEventListener("click", backToChatRoomList);
}

// 채팅창 열기/닫기
function toggleChatContainer() {
	chatContainer.style.display = chatContainer.style.display === "flex" ? "none" : "flex";
}

// 채팅창 닫기
function closeChatContainer() {
	chatContainer.style.display = "none";
}

// Enter 키로 메시지 전송
function handleEnterKey(event) {
	if (event.key === "Enter") {
		sendMessage();
	}
}

// 뒤로가기 버튼 클릭
function backToChatRoomList() {
	document.getElementById("chatRoomList").style.display = "flex";
	document.getElementById('chatRoomList').style.flexDirection = 'column';
	document.getElementById("chatBox").style.display = "none";
	document.querySelector(".chatInput").style.display = "none";
	loadChatRooms();
}

// 페이지 로딩 완료 후 처리
document.addEventListener("DOMContentLoaded", async () => {
	await checkSession(); // 로그인 세션 확인 후 emp_no 저장
	await loadChatRooms(); // 채팅방 목록 불러오기
	setupEventListeners(); // 이벤트 리스너 등록
});

// ✅ 세션 확인 함수
async function checkSession() {
	try {
		const response = await fetch("/auth/session");
		if (response.ok) {
			const data = await response.json();
			if (data.empNo) {
				sessionStorage.setItem("emp_no", data.empNo);
			}
		}
	} catch (error) {
		console.error("❌ 세션 확인 중 오류 발생:", error);
	}
}


// 채팅방 목록 불러오기
async function loadChatRooms() {
	const chatRoomsContainer = document.querySelector("#chatRooms");
	if (!chatRoomsContainer) {
		return;
	}

	const empNo = sessionStorage.getItem("emp_no");
	if (!empNo) {
		return;
	}

	const response = await fetch(`http://localhost:8081/api/chat/rooms/${empNo}`);
	if (response.ok) {
		const chatRooms = await response.json();
		if (chatRooms.length === 0) {
			chatRoomsContainer.innerHTML = "<p>채팅방이 없습니다. 새 채팅방을 만들어주세요!</p>";
		} else {
			renderChatRooms(chatRooms);
		}
	}
}

// 채팅방 목록 렌더링 함수
function renderChatRooms(chatRooms) {
	const chatRoomsContainer = document.getElementById("chatRooms");
	chatRoomsContainer.innerHTML = ''; // 기존 내용 초기화

	chatRooms.forEach(room => {
		const roomElement = document.createElement("div");
		roomElement.className = "chat-room";
		roomElement.id = `room-${room.croomIdx}`;


		const roomTitle = document.createElement("div");
		roomTitle.innerHTML = `<b>${room.createdAt.substring(0, 10)}</b>`;

		// 삭제 버튼
		const deleteButton = document.createElement("span");
		deleteButton.innerText = "❌";
		deleteButton.className = "delete-room-btn";
		deleteButton.onclick = () => deleteChatRoom(room.croomIdx);

		roomTitle.onclick = () => initializeChatRoom(room.croomIdx); // 클릭 시 해당 채팅방으로 이동

		roomElement.appendChild(roomTitle);
		roomElement.appendChild(deleteButton);
		chatRoomsContainer.appendChild(roomElement);
	});
}

// 채팅방 초기화
async function initializeChatRoom(croomIdx) {
	try {
		const empNo = sessionStorage.getItem("emp_no");
		if (!empNo) {
			return;
		}
		sessionStorage.setItem("croomIdx", croomIdx); // 채팅방 ID를 세션에 저장

		switchToChatRoom();  // 채팅방 화면으로 전환

		await loadMessages(croomIdx); // 메시지 로드
	} catch (error) {
		console.error("❌ 채팅방 초기화 중 오류 발생:", error);
	}
}

// 채팅방 전환
function switchToChatRoom() {
	const chatRoomList = document.getElementById("chatRoomList");
	const chatBox = document.getElementById("chatBox");
	const chatInput = document.querySelector(".chatInput");

	// 채팅방 목록 숨기고 채팅방 화면 보이기
	chatRoomList.style.display = "none";
	chatBox.style.display = "flex";
	chatBox.style.flexDirection = "columns"
	chatInput.style.display = "flex"; // 채팅 입력창 보이기
}

// 채팅방 메시지 로딩
// ✅ 메시지 로딩 함수 수정 (JSON 응답 확인)
async function loadMessages(croomIdx) {
	try {
		const response = await fetch(`http://localhost:8081/api/chat/messages/${croomIdx}`);
		const textData = await response.text(); // JSON이 아닐 수도 있으므로 먼저 텍스트로 받기

		// ✅ 응답이 JSON 형식인지 확인
		if (textData.startsWith("{") || textData.startsWith("[")) {

			const messages = JSON.parse(textData); // JSON 변환 시도

			if (messages.length === 0) {
				renderEmptyState();
			} else {
				renderMessages(messages);
			}
		}
	} catch (error) {
		console.error("❌ 메시지 로드 중 오류 발생:", error);
	}
}

// ✅ 채팅 내역이 없을 때 기본 메시지를 표시하는 함수
function renderEmptyState() {
	const chatBox = document.getElementById("chatBox");
	chatBox.innerHTML = ""; // 기존 내용 초기화

	const emptyMessage = document.createElement("div");
	emptyMessage.className = "emptyMessage";
	emptyMessage.innerText = "📭 채팅 내역이 없습니다. 새로운 메시지를 입력하세요!";
	chatBox.appendChild(emptyMessage);
}


// 메시지 렌더링
function renderMessages(messages) {
	const chatBox = document.getElementById("chatBox");
	chatBox.innerHTML = ""; // 기존 메시지 초기화

	// chatIdx를 기준으로 오름차순 정렬 (숫자 비교)
	messages.sort((a, b) => a.chatIdx - b.chatIdx);

	messages.forEach(message => {
		const messageId = message.chatIdx || new Date().getTime();
		const wrapperDiv = addMessageToChatBox({
			chat: message.chat,
			chatIdx: message.chatIdx,
			ratings: message.ratings
		}, message.chatter === "ChatBot" ? "botMessage" : "userMessage");

		// 챗봇 메시지일 경우 별점 적용
		if (message.chatter === "ChatBot" && message.ratings) {
			updateStarUI(wrapperDiv.querySelector(".star-rating"), convertGradeToStars(message.ratings));
		}
	});

	chatBox.scrollTop = chatBox.scrollHeight; // 스크롤을 하단으로 이동
}


// 메시지 추가
function addMessageToChatBox(messageData, messageType) {
	const chatBox = document.getElementById("chatBox");

	const wrapperDiv = document.createElement("div");
	wrapperDiv.className = `${messageType}`;
	wrapperDiv.innerHTML = messageData.chat;

	if (messageType === "userMessage") {
		const horizon = document.createElement("hr");
		horizon.className = "diffline";
		chatBox.appendChild(horizon);
	}

	chatBox.appendChild(wrapperDiv);

	// ✅ 챗봇 메시지일 경우 별점 UI 추가
	if (messageType === "botMessage" && messageData.chatIdx) {
		const starRating = createStarRating(messageData.chatIdx);
		wrapperDiv.appendChild(starRating);

		// 🔹 기존 별점이 있다면 UI 업데이트
		if (messageData.ratings) {
			updateStarUI(starRating, convertGradeToStars(messageData.ratings));
		}
	}

	chatBox.scrollTop = chatBox.scrollHeight;

	return wrapperDiv; // 🔹 메시지 컨테이너 반환 (별점 업데이트용)
}

// 메시지 전송
async function sendMessage() {
	const messageInput = document.getElementById("chatInputField");
	const message = messageInput.value.trim();
	const croomIdx = sessionStorage.getItem("croomIdx");
	const empNo = sessionStorage.getItem("emp_no");

	if (!message) return;

	addMessageToChatBox({ chatter: empNo, chat: message }, "userMessage");
	messageInput.value = "";

	const requestBody = {
		croomIdx: parseInt(croomIdx, 10),
		chatter: empNo,
		chat: message,
		createdAt: new Date().toISOString().split(".")[0]
	};

	try {
		const response = await fetch("http://localhost:8081/api/chat/messages", {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify(requestBody)
		});

		const responseData = await response.json(); // 🔹 JSON 변환 시도
		if (!responseData.chatIdx) {
			return;
		}

		// ✅ 챗봇 응답을 추가하면서, 별점 UI도 함께 생성
		const botMessageDiv = addMessageToChatBox({
			chat: responseData.chat,
			chatIdx: responseData.chatIdx
		}, "botMessage");

	} catch (error) {
		console.error("❌ 메시지 전송 중 오류 발생:", error);
	}
}

// 채팅방 삭제 처리
async function deleteChatRoom(croomIdx) {
	try {
		const response = await fetch(`http://localhost:8081/api/chat/rooms/${croomIdx}`, {
			method: "DELETE",
		});

		if (response.ok) {
			console.log(`✅ 채팅방 ${croomIdx} 삭제됨`);
			const roomElement = document.getElementById(`room-${croomIdx}`);
			if (roomElement) {
				roomElement.remove();
			}
		}
	} catch (error) {
		console.error("❌ 채팅방 삭제 중 오류 발생:", error);
	}
}

// ✅ 채팅방 생성 함수
async function createChatRoom() {
	const empNo = sessionStorage.getItem("emp_no");
	if (!empNo) {
		return;
	}

	try {
		const response = await fetch(`http://localhost:8081/api/chat/createRooms/${empNo}`, {
			method: "POST",
		});

		if (response.ok) {

			// 서버에서 새 채팅방 목록 가져오기
			await loadChatRooms();

			// 최신 채팅방 강조 (애니메이션 효과)
			setTimeout(() => {
				const chatRooms = document.querySelectorAll(".chat-room");
				if (chatRooms.length > 0) {
					chatRooms[chatRooms.length - 1].classList.add("highlight"); // 최신 채팅방 강조
					setTimeout(() => chatRooms[chatRooms.length - 1].classList.remove("highlight"), 2000);
				}
			}, 500);
		}
	} catch (error) {
		console.error("❌ 채팅방 생성 중 오류 발생:", error);
	}
}

// 평점
//
//
//

// ✅ 별점 UI 생성 함수
function createStarRating(messageId) {
	const ratingContainer = document.createElement("div");
	ratingContainer.classList.add("star-rating");

	for (let i = 5; i >= 1; i--) {
		const star = document.createElement("span");
		star.classList.add("star");
		star.innerHTML = "★";
		star.dataset.value = i;

		// ⭐ 별점 클릭 이벤트 추가
		star.addEventListener("click", async () => {
			await sendRatingToServer(messageId, i);
			updateStarUI(ratingContainer, i);
		});

		ratingContainer.appendChild(star);
	}

	return ratingContainer;
}

// ✅ 별점 UI 업데이트 함수
function updateStarUI(ratingContainer, rating) {
	const stars = ratingContainer.querySelectorAll(".star");
	stars.forEach(star => {
		star.classList.remove("selected");
		if (star.dataset.value <= rating) {
			star.classList.add("selected");
		}
	});
}

async function sendRatingToServer(messageId, rating) {
	try {
		const response = await fetch("/api/chat/rating", {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify({ messageId, rating })
		});
		if (response.ok) {
			return;
		}
	} catch (error) {
		console.error("❌ 별점 전송 오류:", error);
	}
}

function convertGradeToStars(grade) {
	switch (grade) {
		case "A": return 5;
		case "B": return 4;
		case "C": return 3;
		case "D": return 2;
		case "E": return 1;
		default: return 0;
	}
}

