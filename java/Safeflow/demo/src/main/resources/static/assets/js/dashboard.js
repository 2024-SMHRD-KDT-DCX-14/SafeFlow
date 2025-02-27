// 전역 변수
var page = 0; // 기본 페이지 0
var pageSize = 5; // 기본 페이지 크기 5
var totalPages = 0; // 전역 변수로 선언

/* 초기화 함수 */
function init() {
	getDocuTop5(); // 최신 개정된 게시판 상위 5개 
	dataload(); // line chart 그리기
	getRandomSafetyQuote(); // 랜덤 글귀
	calculateNoAccidentDays(); // 무사고 일수 계산
	unconfirmedEvent(page, pageSize); // 미확인 이벤트 페이징
}

init();

// 1. 게시판 상위 5개 불러오기
async function getDocuTop5() {

	try {

		const response = await fetch("/dashboard/topboard");

		if (!response.ok) {
			throw new Error("상위 5개 문서 조회에 실패했습니다.");
		}

		const data = await response.json();

		// 테이블 가져오기
		const tbody = document.querySelector("#recent-document-tbody");

		tbody.innerHTML = "";
		// 게시판 인덱스
		let index = 1;

		data.forEach(doc => {
			const row = document.createElement("tr");
			row.innerHTML = `
				
				<td>${index++}</td>
				<td class="text-truncate" style="max-width: 100px; width:100px;" >${doc.docuType}</td>
				<td class="text-truncate" style="max-width: 400px; width:400px;" >${doc.docuNm}</td>
				<td>${doc.createdAt}</td>
				
				`;
			tbody.appendChild(row);
		});

	} catch (error) {
		console.error("문서 조회 중 오류 발생:", error);
	}
}


// 2번 (bar Chart 그래프) - Chart.js를 사용한 원형 차트

function dataload() {
	const response = fetch("/dashboard/eventchart", {
		method: "POST"
	});
	response.then(response => {
		if (!response.ok) {
			throw new Error("데이터 로드 실패");
		}

		return response.json();
	})
		.then(data => {
			const months = [];
			const eventCounts = [];

			data.forEach(item => {
				months.push(`${item[0]}-${item[1]}`); // 2024-01 형태로 연월 표시
				eventCounts.push(item[2]);  // eventCount 값 추출
			})

			// 차트그리기
			const ctx = document.getElementById('bar-chart').getContext('2d');

			const barchart = new Chart(ctx, {
				type: 'bar', // 차트 유형
				data: {
					labels: months, // X축 레이블
					datasets: [{
						label: "Event Count",
						data: eventCounts, // Y 축 데이터
						backgroundColor: 'rgba(54, 162, 235, 0.2)', // 막대 내부 색상
						borderColor: 'rgba(54, 162, 235, 1)',
						borderWidth: 1 // 테두리 두께
					}]
				},
				options: {
					scale: {
						y: { beginAtZero: true } // y축 0부터 시작
					},
					responsive: true, // 반응형 크기 조절 가능
					maintainAspectRatio: false // 가로세로 비율 유지 여부 (false: 비율 유연)
				}
			});

		}).catch(error => {
			console.error("Error fetching data:", error);
		});

}

// 3번 무사고 일수 계산, 랜덤으로 글자 띄우기
// 3-1 랜덤으로 글자 띄우기
function getRandomSafetyQuote() {

	// 1📜 "오늘의 안전" - 랜덤 글귀 목록
	const safetyQuotes = [
		"방심은 사고로, 주의는 안전으로 이어진다.",
		"작은 실천이 큰 안전을 만든다.",
		"당신의 한 걸음이 모두의 안전을 지킨다.",
		"안전은 우리가 함께 만들어가는 약속입니다.",
		"작은 불편이 큰 사고를 막는다.",
		"위험을 예측하고 예방하는 것이 최선의 안전 대책이다.",
		"오늘도 무사히, 모두의 안전을 위해!"
	];

	const randomIndex = Math.floor(Math.random() * safetyQuotes.length)


	const randomQuotes = document.getElementById("random-quote");
	randomQuotes.innerHTML = safetyQuotes[randomIndex];

}

// 3-2 안전 무사고 일수 현황 계산
function calculateNoAccidentDays() {

	const current_situation = document.getElementById("current_situation");

	const response = fetch("/dashboard/calculateNoAccident")
	response.then(response => {

		if (!response.ok) {
			throw new Error("계산 데이터 로드 실패");
		}
		return response.json();
	}).then(data => {
		current_situation.innerHTML = `현재 무사고 <b>${data}일</b>째 입니다!`;;
	})
}

// 4번 미확인 이벤트
async function unconfirmedEvent(page, pageSize) {

	const response = await fetch(`/dashboard/unconfirmedEvent?page=${page}&size=${pageSize}`);

	if (!response.ok) {
		throw new Error("미확인 이벤트 출력실패");
	}

	const data = await response.json();

	// 전체 페이지 수
	totalPages = data.totalPages;

	// 총 미확인 건수
	const total = document.getElementById("total");
	total.innerHTML ='<span>미확인 이벤트 수: </span>';
	if(data.totalElements > 0){
		total.innerHTML += `<b style="color:red">${data.totalElements}<b>`;
	}else{
		total.innerHTML += `<b style="color:green">${data.totalElements}<b>`;
	}
	
	

	// 테이블 가져오기
	const tbody = document.getElementById("event-tbody");
	tbody.innerHTML = "";
	
	// DocumentFragment 생성으로 성능 향상
	const fragment = document.createDocumentFragment();

	data.content.forEach(event => {
		const row = document.createElement("tr");
		row.innerHTML = `
					<td>${event.eventIdx}</td>
					<td>${event.createdAt}</td>
					<td>${event.eventType}</td>
					<td class="text-truncate" style="max-width: 200px;" >${event.cctvLoc}</td>
					<td>${event.doneYn}</td>
					<td><span class="modalOpenButton" style="cursor:pointer;"data-event="${JSON.stringify(event).replace(/"/g, '&quot;')}">확인</span></td>
					`;
		// data는 서버에서 받은 전체 JSON 응답
		// data.content는 이벤트 목록을 담은 배열
		// event는 data.content 배열에서 하나씩 꺼낸 개별 이벤트 데이터

		fragment.appendChild(row);
	})

	tbody.appendChild(fragment); // 한번에 돔에 추가

	// 페이지 네비게이션 업데이트
	updatePagination(data);


};

// 이벤트 위임 방식으로 "확인" 버튼 클릭 시 모달 열기
// e는 클릭 이벤트 객체 자동으로 생성됨, 자동 실행됨!
document.getElementById("event-tbody").addEventListener("click", (e) => {
	if (e.target.classList.contains("modalOpenButton")) {
		
		try {
			// JSON.parse -> JSON 객체로 변환해서
			const eventData = JSON.parse(e.target.getAttribute("data-event"));
			openModal(eventData);
			
		} catch (error) {
			alert("이벤트 데이터를 불러오는 중 오류가 발생했습니다.");
		}
	}
});


// 이벤트 위임 방식으로 "확인" 버튼 클릭 시 모달 열기
document.getElementById("event-tbody").addEventListener("click", (e) => {
	if (e.target.classList.contains("modalOpenButton")) {
		try {
			const eventData = JSON.parse(e.target.getAttribute("data-event"));
			openModal(eventData);
		} catch (error) {
			alert("이벤트 데이터를 불러오는 중 오류가 발생했습니다.");
		}
	}
});

// 모달 창 열기 함수
function openModal(eventData) {
	const modal = document.getElementById("modalContainer");

	const modalContent = document.getElementById("modal-text");
	
	// 모달에 이벤트 데이터 추가
	modalContent.innerHTML = `
	  <table style="width: 100%; height:400px; background-color: #fff; border-collapse: collapse;">
	    <tr style="height:30px; border-top: 1px solid #ddd; border-bottom: 1px solid #ddd;">
	      <th style="padding: 4px; text-align: left;">이벤트 번호</th>
	      <td style="padding: 4px;">${eventData.eventIdx}</td>
	    </tr>
	    <tr style="height:30px; border-top: 1px solid #ddd; border-bottom: 1px solid #ddd;">
	      <th style="padding: 4px; text-align: left;">이벤트 유형</th>
	      <td style="padding: 4px;">${eventData.eventType}</td>
	    </tr>
	    <tr style="height:30px; border-top: 1px solid #ddd; border-bottom: 1px solid #ddd;">
	      <th style="padding: 4px; text-align: left;">발생 CCTV</th>
	      <td style="padding: 4px;">${eventData.cctvLoc}</td>
	    </tr>
	    <tr style="height:30px; border-top: 1px solid #ddd; border-bottom: 1px solid #ddd;">
	      <th style="padding: 4px; text-align: left;">발생 시간</th>
	      <td style="padding: 4px;">${eventData.createdAt}</td>
	    </tr>
	    <tr style="height:250px; border-top: 1px solid #ddd; border-bottom: 1px solid #ddd;">
	      <th style="padding: 4px; text-align: left;">내용</th>
	      <td style="padding: 4px;">
			<textarea class="content" placeholder="작성이 필요합니다."
			          style="
			            width: 100%;
			            min-height: 80px;
						height : 100%;
			            padding: 8px;
			            border: none;
			            border-bottom: 1px solid #ccc;
			            background-color: transparent;
			            resize: none; 
			            outline: none;
			            box-sizing: border-box;
			            font-size: 14px;
						
			          "
					  ></textarea>
	      </td>
	    </tr>
	  </table>
	`;
	
	const filePath = eventData.eventVideo; // 예: "C:\videos\CCTV_184_head_20250222_110131.output.mp4"
	const encodedPath = encodeURIComponent(filePath);
	fetch(`/dashboard/video?filePath=${encodedPath}`)
	  .then(response => response.blob())
	  .then(blob => {
	    const videoURL = URL.createObjectURL(blob);
		console.log(videoURL);
	    document.getElementById('eventVideo').src = videoURL;
	  });


	
	

	// 모달 열기
	modal.classList.remove("hidden");

	// 모달 닫기 이벤트 추가 (한 번만 실행되도록 설정)
	document.getElementById("closeModal").addEventListener("click", () => {
		modal.classList.add("hidden");
	}, { once: true });
	
	// 등록 완료해서 업데이트 되도록
	document.getElementById("write").addEventListener("click", async() => {
	    const contentElement = document.querySelector(".content");
	    if (!contentElement.value.trim()) {
	        alert("내용을 입력해주세요.");
	        return;
	    }
	    await updateEventContetnt(eventData.eventIdx, contentElement.value);
	    // 모달 숨기기
	    document.getElementById("modalContainer").classList.add("hidden");
	});
}

// 업데이트 요청을 서버에 보내는 함수
async function updateEventContetnt(eventIdx, content){
	
	try{
		const response = await fetch("/dashboard/uploadContent", {
			method : 'POST',
			headers: {
				'Content-Type': 'application/json'
			},
			body: JSON.stringify({
				eventIdx: eventIdx, // 이벤트 고유 idx
				content: content  // 작성한 내용
			})
			
		});
		
		if(!response.ok){
			throw new Error("이벤트 업데이트 실패");
		}
		
		alert("이벤트 수정 완료되었습니다.");
		
		page=0;
		unconfirmedEvent(page, pageSize);
		
		
	} catch(error){
		alert("이벤트 업데이트 중 오류가 발생했습니다.");
	}
	
}



// 페이지네이션 업데이트
function updatePagination(data) {
	const prevBtn = document.getElementById("event-prevBtn");
	const nextBtn = document.getElementById("event-nextBtn");

	// 이전 버튼 활성화 여부
	prevBtn.disabled = data.first; // 첫 페이지일 경우 비활성화
	// 다음 버튼 활성화 여부
	nextBtn.disabled = data.last;

	// 페이지 변경 버튼 클릭 시 페이지 변경
	prevBtn.onclick = () => changePage(data.number - 1); // 이전 페이지로 이동
	nextBtn.onclick = () => changePage(data.number + 1); // 다음 페이지로 이동

}

// 페이지 변경 함수
function changePage(newPage) {
	if (newPage < 0 || newPage >= totalPages) return; // 페이지 번호 범위 체크
	page = newPage; // 페이지 갱신
	unconfirmedEvent(page, pageSize); // 새로운 페이지로 문서 초기화
}

