// ===================
//  모달 관련 기능
// ===================
var modalOpenButton = document.getElementById('modalOpenButton');
var modalCloseButton = document.getElementById('modalCloseButton');
var modal = document.getElementById('modalContainer');

// 모달 오픈 버튼
modalOpenButton.addEventListener('click', () => {
	modal.classList.remove('hidden');
});

// 모달 닫기 버튼
modalCloseButton.addEventListener('click', () => {
	modal.classList.add('hidden');
});

// 초기화 함수
function init() {

	setupCloseViewer();   // 뒤로가기 버튼 설정
	setFileChange();       // 파일 입력 이벤트 등록
	initializeDocuments(page, pageSize); // 전체 문서 조회
}

// 초기화 호출
init();


// ===================
//  파일 업로드 및 전송 기능
// ===================
document.getElementById("submit").addEventListener("click", async function() {

	const fileInput = document.getElementById("file"); // 파일
	const titleInput = document.getElementById("title").value; // 문서명(파일명)
	const writerInput = document.getElementById("writer").value; // 작성자(세션아이디)
	const dateInput = document.getElementById("date").value; // 문서승인일자
	const doctypeInput = document.getElementById("doctype").value;

	// dateInput 날짜(date로 형변환)
	const dateObject = new Date(dateInput);
	// toISOString()은 "2025-02-11T00:00:00.000Z" 형식으로 반환되므로 
	// split("T")[0]을 사용해 yyyy-MM-dd만 가져옴. 결과: 2025-02-11
	const formatteddate = dateObject.toISOString().split("T")[0];

	if (fileInput.files.length === 0) {
		alert("파일을 선택하세요.");
		return;
	}

	const file = fileInput.files[0];

	// Step 1: 파일 업로드
	const formData = new FormData();
	formData.append("file", file);
	formData.append("doctype", doctypeInput);

	try {
		const uploadResponse = await fetch("/uploadFile", {
			method: "POST",
			body: formData
		});

		if (!uploadResponse.ok) throw new Error("파일 업로드 실패");

		const uploadResult = await uploadResponse.json();
		const fileUrl = uploadResult.fileUrl; // 서버에서 반환한 파일 URL

		// Step 2: JSON 데이터 전송
		const requestBody = {
			title: titleInput,
			writer: writerInput,
			date: formatteddate,
			fileUrl: fileUrl,
			category: doctypeInput
		};

		const saveResponse = await fetch("/upload", {
			method: "POST",
			body: JSON.stringify(requestBody),
			headers: { "Content-Type": "application/json" }
		});

		if (!saveResponse.ok) throw new Error("데이터 저장 실패");

		// ✅ 모달 닫기
		modal.classList.add('hidden');
		clearInputs();


		alert("업로드 완료!");

	} catch (error) {
		console.error("에러 발생:", error);
		alert("업로드 중 오류 발생!");
	}
});


// 모달 입력 필드 초기화 함수
function clearInputs() {
	document.getElementById("title").value = "";
	document.getElementById("date").value = "";

	// 파일 입력 필드 초기화
	const fileInput = document.getElementById("file");
	fileInput.value = null; // 파일 초기화

	// 파일 입력 필드를 새롭게 교체하여 완전 초기화
	const newFileInput = fileInput.cloneNode(true);
	fileInput.replaceWith(newFileInput);

	// 새로운 파일 입력 필드에 이벤트 리스너 다시 등록
	setFileChange();
}


// 파일 선택 시 파일명을 자동 입력 함수
function setFileChange() {

	const fileInput = document.getElementById("file");

	fileInput.addEventListener("change", function() {

		const titleInput = document.getElementById("title");
		if (fileInput.files.length > 0) {
			titleInput.value = fileInput.files[0].name;
		}

	});
}

// ===================
//  전체 문서 조회 (게시판 초기화)
// ===================


// 페이지 변수 정의 (기본값 설정)
var page = 0; // 기본 페이지 0
var pageSize = 10; // 기본 페이지 크기 10
var totalPages = 0; // 전역 변수로 선언


// 문서 게시판 접속시 전체문서 조회
function initializeDocuments(page, pageSize) {

	const body = document.getElementById("body");

	// page와 pageSize 값이 NaN이나 undefined인 경우 기본값으로 설정
	if (isNaN(page) || page === undefined) {
		page = 0; // 기본값 0
	}
	if (isNaN(pageSize) || pageSize === undefined) {
		pageSize = 10; // 기본값 10
	}


	try {
		// 페이지 로드 시, 전체 문서 목록을 기본적으로 가져옵니다.
		const response = fetch(`/api/documents/all?page=${page}&size=${pageSize}`);
		response.then(response => {
			if (!response.ok) {
				throw new Error("전체 문서 목록을 가져오는 데 실패했습니다.");
			}
			return response.json();


		}).then(data => {


			totalPages = data.totalPages;  // totalPages 값 확인

			// 테이블 생성
			const template = document.getElementById("document-table-template");
			const clone = template.content.cloneNode(true);

			const tableBody = clone.querySelector(".file-table-body");

			// tableBody가 null인 경우, 오류 처리 추가
			if (!tableBody) {
				throw new Error("테이블 본문을 찾을 수 없습니다.");
			}

			// 전체 제목 설정
			const title = document.getElementById("category-title");
			title.innerHTML = "전체";

			let index = page * pageSize + 1;

			// 전체 카테고리에서 파일들을 하나의 테이블에 추가
			// Object.entries(categories) : categories 객체: key(키) : [배열] 출력되는 객체
			data.content.forEach(item => {
				const { documents, category } = item;
				const row = document.createElement("tr");
				row.innerHTML = `
			        <td>${index++}</td>
			        <td>${category}</td>
			        <td class="file-item" data-category="${category}" data-filename="${documents}">${documents}</td>
			    `;

				// 인덱스 열의 너비를 5%로 설정
				row.querySelector("td:nth-child(1)").style.width = "5%";
				// 카테고리명 열의 너비를 95%로 설정
				row.querySelector("td:nth-child(2)").style.width = "15%";
				// 문서명 열의 너비를 95%로 설정
				row.querySelector("td:nth-child(3)").style.width = "85%";
				// 문서명 열의 텍스트를 왼쪽 정렬
				row.querySelector("td:nth-child(3)").style.textAlign = "left";

				tableBody.appendChild(row);
			});


			// 기존 내용 초기화 후 테이블 추가
			body.innerHTML = "";
			body.appendChild(clone);

			// 파일명 클릭 이벤트 추가 (PDF 보기 기능)
			document.querySelectorAll(".file-item").forEach(item => {
				item.addEventListener("click", function() {
					viewPDF(this);
				});
			});

			updatePaginationControls(data);

		}).catch(error => {
			console.error(error);
			body.innerHTML = `<p>전체 문서 목록을 가져오는 데 오류가 발생했습니다.</p>`;
		});

	} catch (error) {
		console.error(error);
		body.innerHTML = `<p>전체 문서 목록을 가져오는 데 오류가 발생했습니다.</p>`;
	}
}


// 페이지네이션 버튼 업데이트
function updatePaginationControls(data) {
	const paginationNumber = document.getElementById("pagination-number");

	// 페이지 버튼들 생성 및 업데이트
	const prevBtn = document.getElementById("prev-btn");
	const nextBtn = document.getElementById("next-btn");

	if (paginationNumber) {
		paginationNumber.innerHTML = `${data.number + 1} of ${data.totalPages}`;
	}

	// 이전 버튼 활성화 여부
	prevBtn.disabled = data.first;
	// 다음 버튼 활성화 여부
	nextBtn.disabled = data.last;

	// 페이지 변경 버튼 클릭 시 페이지 변경
	prevBtn.onclick = () => changePage(data.number - 1);
	nextBtn.onclick = () => changePage(data.number + 1);

}

// 페이지 변경 함수
function changePage(newPage) {
	if (newPage < 0 || newPage >= totalPages) return;
	page = newPage; // 페이지 갱신
	initializeDocuments(page, pageSize);
}

// 카테고리 클릭 처리 하기
var currentCategory = null; // 현재 선택된 카테고리 상태 변수


// 문서 카테고리 클릭하면 리스트 출력
document.querySelectorAll(".list").forEach(folder => {
	folder.addEventListener("click", async function() {

		const category = this.getAttribute("data-category");
		const body = document.getElementById("body");

		// 📌 ✅ 카테고리를 변경할 때 페이지 초기화
		if (category !== currentCategory) {
			page = 0;  // ✅ 무조건 1페이지(0)부터 시작하도록 초기화
		}

		// 폴더 아이콘을 클릭했을 때 폴더 아이콘 변경
		if (this.textContent.startsWith("📁")) {
			this.textContent = '📂 ' + this.textContent.slice(2);  // 아이콘을 📂로 변경
		} else {
			this.textContent = '📁 ' + this.textContent.slice(2);  // 아이콘을 📁로 변경
		}

		// 동일한 카테고리가 클릭되면 전체 문서 조회로 돌아감
		if (category === currentCategory) {
			page = 0;
			initializeDocuments(page, pageSize);
			currentCategory = null; // 카테고리 상태 초기화

			return;
		}

		// 카테고리 변경
		currentCategory = category;

		// 카테고리 문서 조회
		try {
			const response = await fetch(`/api/documents/category/${category}?page=${page}&size=${pageSize}`);
			if (!response.ok) {
				throw new Error("문서 목록을 가져오는 데 실패했습니다.");
			}

			const data = await response.json();

			// 템플릿 요소 복사
			const template = document.getElementById("document-table-template");
			const clone = template.content.cloneNode(true);

			// 클론 내부에서 특정 요소(예: th.cate)를 삭제
			const cateHeader = clone.querySelector("th.cate");
			if (cateHeader) {
				cateHeader.remove();
			}

			// 카테고리 제목 설정
			const title = document.getElementById("category-title");
			title.innerHTML = category;

			// 테이블 본문 선택
			const tableBody = clone.querySelector(".file-table-body");

			// 파일 리스트 추가
			data.content.forEach((docuNm, index) => {
				const row = document.createElement("tr");
				row.innerHTML = `
                    <td>${index + 1 + page * pageSize}</td>
                    <td class="file-item" data-category="${category}" data-filename="${docuNm}">${docuNm}</td>
                `;

				// 인덱스 열의 너비를 5%로 설정
				row.querySelector("td:nth-child(1)").style.width = "5%";
				// 문서명 열의 너비를 95%로 설정
				row.querySelector("td:nth-child(2)").style.width = "95%";
				// 문서명 열의 텍스트를 왼쪽 정렬
				row.querySelector("td:nth-child(2)").style.textAlign = "left";

				tableBody.appendChild(row);
			});

			// 기존 내용 초기화 후 삽입
			body.innerHTML = "";
			body.appendChild(clone);

			// 페이지네이션 버튼 업데이트
			updateCategoryPaginationControls(data);

			// 파일명 클릭 이벤트 추가 (PDF 보기 기능)
			document.querySelectorAll(".file-item").forEach(item => {
				item.addEventListener("click", function() {
					viewPDF(this);
				});
			});

		} catch (error) {
			console.error(error);
			body.innerHTML = `<p>문서 목록을 가져오는 데 오류가 발생했습니다.</p>`;
		}
	});
});

// 카테고리 별 페이지네이션 버튼 업데이트
function updateCategoryPaginationControls(data) {
	const paginationNumber = document.getElementById("pagination-number");

	// 페이지 버튼들 생성 및 업데이트
	const prevBtn = document.getElementById("prev-btn");
	const nextBtn = document.getElementById("next-btn");

	// 페이지 번호 텍스트 업데이트
	if (paginationNumber) {
		paginationNumber.innerHTML = `${data.number + 1} of ${data.totalPages}`;
	}

	// 이전 버튼 활성화 여부
	prevBtn.disabled = data.first;
	// 다음 버튼 활성화 여부
	nextBtn.disabled = data.last;

	// 페이지 변경 버튼 클릭 시 페이지 변경
	prevBtn.onclick = () => changeCategoryPage(data.number - 1);
	nextBtn.onclick = () => changeCategoryPage(data.number + 1);
}


// 카테고리 문서페이지 변경 함수
function changeCategoryPage(newPage) {

	if (newPage < 0 || newPage >= totalPages) {
		return;
	}
	currentPage = newPage; // 페이지 갱신
	loadCategoryDocuments(currentCategory, currentPage, pageSize);
}

// 카테고리 문서 목록 조회 및 페이징 처리
async function loadCategoryDocuments(category, page, pageSize) {
	const body = document.getElementById("body");

	try {
		const response = await fetch(`/api/documents/category/${category}?page=${page}&size=${pageSize}`);
		if (!response.ok) {
			throw new Error("문서 목록을 가져오는 데 실패했습니다.");
		}

		const data = await response.json();

		// totalPages 값을 저녁 변수에 설정
		totalPages = data.totalPages;

		// 템플릿 요소 복사
		const template = document.getElementById("document-table-template");
		const clone = template.content.cloneNode(true);

		// 클론 내부에서 특정 요소(예: th.cate)를 삭제
		const cateHeader = clone.querySelector("th.cate");
		if (cateHeader) {
			cateHeader.remove();
		}

		// 카테고리 제목 설정
		const title = document.getElementById("category-title");
		title.innerHTML = category;

		// 테이블 본문 선택
		const tableBody = clone.querySelector(".file-table-body");

		// 파일 리스트 추가
		data.content.forEach((docuNm, index) => {
			const row = document.createElement("tr");
			row.innerHTML = `
                <td>${index + 1 + page * pageSize}</td>
                <td class="file-item" data-category="${category}" data-filename="${docuNm}">${docuNm}</td>
            `;
			// 인덱스 열의 너비를 5%로 설정
			row.querySelector("td:nth-child(1)").style.width = "5%";
			// 문서명 열의 너비를 95%로 설정
			row.querySelector("td:nth-child(2)").style.width = "95%";
			// 문서명 열의 텍스트를 왼쪽 정렬
			row.querySelector("td:nth-child(2)").style.textAlign = "left";

			tableBody.appendChild(row);

		});

		// 기존 내용 초기화 후 삽입
		body.innerHTML = "";
		body.appendChild(clone);

		// 페이지네이션 버튼 업데이트
		updateCategoryPaginationControls(data);

		// 파일명 클릭 이벤트 추가 (PDF 보기 기능)
		document.querySelectorAll(".file-item").forEach(item => {
			item.addEventListener("click", function() {
				viewPDF(this);
			});
		});

	} catch (error) {
		console.error(error);
		body.innerHTML = `<p>문서 목록을 가져오는 데 오류가 발생했습니다.</p>`;
	}
}



// ===================
//  PDF 보기 기능
// ===================

var pdfcurrentPageNum = 1;

// PDF 파일을 로드하는 함수(PDF.js 사용)
function loadPDF(encodedCategory, encodedFilename) {
	return fetch(`/api/documents/${encodedCategory}/${encodedFilename}`)
		.then(response => response.arrayBuffer()) // PDF를 바이너리 데이터로 가져오기
		.then(data => {
			return pdfjsLib.getDocument(data).promise; // PDF.js에서 처리할 수 있는 문서 객체를 반환함.
		})
		.catch(error => console.error("Error fetching PDF:", error));
}

// PDF 읽어오기 함수
function viewPDF(element) {
	// PDF.js 워커 파일 경로 설정
	pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/2.10.377/pdf.worker.min.js';

	const category = element.getAttribute("data-category");
	const filename = element.getAttribute("data-filename");

	// 한글 카테고리 & 파일명 URL 인코딩
	const encodedCategory = encodeURIComponent(category);
	const encodedFilename = encodeURIComponent(filename);

	// PDF 파일을 로드하고 렌더링
	loadPDF(encodedCategory, encodedFilename).then(pdf => {
		const numPages = pdf.numPages;

		// PDF 뷰어 UI 변경
		togglePDFViewerUI();

		// 모든 페이지를 한 번에 로드
		for (let i = 1; i <= numPages; i++) {
			renderPage(i, pdf); // 페이지 렌더링
		}

	}).catch(function(error) {
	});
}

// PDF 뷰어 UI 보이기/숨기기 (문서명 클릭하면 PDF display)
function togglePDFViewerUI() {
	document.getElementById("boardmain").style.display = "none"; // 게시판 숨기기
	document.getElementById("pdf-viewer-container").style.display = "block"; // PDF 뷰어 보이기
}

// PDF 페이지 렌더링 함수
function renderPage(pageNum, pdfDoc) {
	pdfDoc.getPage(pageNum).then(function(page) {
		const scale = 1.5; // 확대 비율 설정
		const viewport = page.getViewport({ scale: scale });

		// 페이지마다 캔버스를 생성하여 렌더링
		const canvas = document.createElement("canvas"); // <canvas>: PDF 페이지를 렌더링할 공간 제공
		const context = canvas.getContext("2d");
		canvas.width = viewport.width;
		canvas.height = viewport.height;

		// PDF 페이지를 캔버스에 렌더링
		page.render({
			canvasContext: context,
			viewport: viewport
		});

		// 페이지 캔버스를 PDF 페이지 컨테이너에 추가
		const pdfPagesContainer = document.getElementById("pdf-pages-container");
		pdfPagesContainer.appendChild(canvas);
	});
}

// 뒤로가기 버튼 설정
function setupCloseViewer() {
	const closeViewerButton = document.getElementById("closeViewerButton");
	closeViewerButton.addEventListener("click", () => {
		document.getElementById("pdf-viewer-container").style.display = "none";
		document.getElementById("boardmain").style.display = "block"; // 게시판 다시 보이기
		document.getElementById("pdf-pages-container").innerHTML = ''; // PDF 페이지 삭제
	});
}


// ===================
//  검색 기능 (API 기반)
// ===================
var searchInput = document.getElementById("document_title");
var searchButton = document.getElementById("search");

// 검색 실행 함수
async function searchDocuments() {

	const text = document.getElementById("document_title").value.trim();
	if (!text) {
		alert("검색어를 입력하세요.");
		return;
	}

	const body = document.getElementById("body");

	try {
		const response = await fetch(`/api/documents/search?text=${text}&page=${page}&size=${pageSize}`);

		if (!response.ok) {
			throw new Error("문서 검색에 실패했습니다.");
		}

		const searchResults = await response.json(); // 검색 결과 JSON 변환

		// totalPages 값을 저녁 변수에 설정
		totalPages = searchResults.totalPages;

		// 테이블 생성
		const template = document.getElementById("document-table-template");
		const clone = template.content.cloneNode(true);
		const tableBody = clone.querySelector(".file-table-body");

		let index = page * pageSize + 1; // 번호 시작

		// 검색시 제목 태그 초기화
		const title = document.getElementById("category-title");
		title.innerHTML = "";

		// 검색 결과를 기존 테이블 형식으로 추가
		searchResults.content.forEach(({ docuType, docuNm }) => {
			const row = document.createElement("tr");
			row.innerHTML = `
		        <td>${index++}</td>
		        <td>${docuType}</td>
		        <td class="file-item" data-category="${docuType}" data-filename="${docuNm}">${docuNm}</td>
		    `;
			tableBody.appendChild(row);
		});

		// 기존 내용 초기화 후 검색 결과 테이블 추가
		body.innerHTML = "";
		body.appendChild(clone);

		// 검색 페이지네이션 버튼 업데이트
		updateSearchPaginationControls(searchResults);

		// 파일명 클릭 이벤트 추가 (PDF 보기 기능)
		document.querySelectorAll(".file-item").forEach(item => {
			item.addEventListener("click", function() {
				viewPDF(this);
			});
		});

	} catch (error) {
		console.error("문서 검색 오류:", error);
		body.innerHTML = `<p>문서 검색 중 오류가 발생했습니다.</p>`;
	}
}

// 페이지네이션 버튼 업데이트
function updateSearchPaginationControls(searchResults) {

	const prevBtn = document.getElementById("prev-btn");
	const nextBtn = document.getElementById("next-btn");
	const paginationNumber = document.getElementById("pagination-number");

	// 페이지 번호 텍스트 업데이트
	if (paginationNumber) {
		paginationNumber.innerHTML = `Page ${searchResults.number + 1} of ${searchResults.totalPages}`;
	}

	// 이전 버튼 활성화 여부
	prevBtn.disabled = searchResults.first;
	// 다음 버튼 활성화 여부
	nextBtn.disabled = searchResults.last;

	// 페이지 변경 버튼 클릭 시 페이지 변경
	prevBtn.onclick = () => changeSearchPage(searchResults.number - 1);
	nextBtn.onclick = () => changeSearchPage(searchResults.number + 1);
}

// 페이지 변경 함수
function changeSearchPage(newPage) {

	// 페이지 번호가 유효한 범위 내에 있는지 체크
	if (newPage < 0 || newPage >= totalPages) return;

	page = newPage; // 페이지 갱신
	searchDocuments(page);

}


// 검색 버튼 클릭 이벤트
searchButton.addEventListener("click", function() {
	// 입력시 page는 0으로 초기화
	page = 0;
	searchDocuments(page);
});

// 엔터 키 입력 이벤트
searchInput.addEventListener("keypress", function(event) {
	if (event.key === "Enter") {
		// 입력시 page는 0으로 초기화
		page = 0;
		searchDocuments(page);
	}
});





