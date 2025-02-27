document.addEventListener("DOMContentLoaded", function() {
  const content = document.querySelector("content");
  init();
  
  // 초기화 함수
  function init() {
    loadMain();
  }
  
  // 메뉴 요소 가져오기
  const mainMenu = document.querySelector("main");
  const boardMenu = document.querySelector("board");
  const myPage = document.querySelector("mypage");

  // 스크립트 로드 함수: 같은 src가 있으면 제거 후 새로 추가
  function loadScript(src, options = {}) {
    const existingScript = document.querySelector(`script[src="${src}"]`);
    if (existingScript) {
      existingScript.remove();
    }
    const script = document.createElement("script");
    script.src = src;
    if (options.defer) {
      script.defer = true;
    }
    if (options.id) {
      script.id = options.id;
    }
    document.body.appendChild(script);
  }

  // 메인 화면 로드 함수
  function loadMain() {
    fetch("/dashboard")
      .then(response => response.text())
      .then(data => {
        content.innerHTML = data;
        loadDashboardScript();
        loadChartJS();
      })
      .catch(error => console.error("Error loading dashboard:", error));
  }

  // 메인 화면 dashboard.js 로드 함수
  function loadDashboardScript() {
    loadScript("assets/js/dashboard.js");
  }

  // 차트 시각화 라이브러리 로드 함수
  function loadChartJS() {
    loadScript("https://cdn.jsdelivr.net/npm/chart.js", { defer: true });
    loadScript("https://cdn.jsdelivr.net/npm/date-fns", { defer: true });
    loadScript("https://cdn.jsdelivr.net/npm/chartjs-adapter-date-fns", { defer: true });
  }

  // 게시판 화면 로드 함수
  function loadBoard() {
    fetch("/board")
      .then(response => response.text())
      .then(data => {
        content.innerHTML = data;
        loadBoardScript();
        loaPDFdScript();
      })
      .catch(error => console.error("Error loading board:", error));
  }

  // board.js 동적 로드 함수
  function loadBoardScript() {
    loadScript("assets/js/board.js", { defer: true });
  }

  // board PDF Script 로드 함수
  function loaPDFdScript() {
    loadScript("https://cdnjs.cloudflare.com/ajax/libs/pdf.js/2.10.377/pdf.min.js", { defer: true });
  }

  // 마이페이지 로드 함수
  function loadMypage() {
    fetch("/myPage")
      .then(response => response.text())
      .then(data => {
        content.innerHTML = data;
        loadMypageScript();
      })
      .catch(error => console.error("Error loading myPage:", error));
  }

  // myPage.js 동적 로드 함수
  function loadMypageScript() {
    loadScript("assets/js/myPage.js");
  }

  // 이벤트 리스너 추가
  myPage.addEventListener("click", loadMypage);
  mainMenu.addEventListener("click", loadMain);
  boardMenu.addEventListener("click", loadBoard);
});
