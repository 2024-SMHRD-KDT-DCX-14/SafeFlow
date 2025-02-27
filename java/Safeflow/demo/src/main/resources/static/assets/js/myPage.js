function toggleEdit() {
	let inputs = Array.from(document.getElementsByClassName("input-data"));
	let editBtn = document.getElementById("editBtn");
	let saveBtn = document.getElementById("saveBtn");

	inputs.forEach(input => input.removeAttribute("readonly")); // 입력 가능하게 변경
	editBtn.classList.add("d-none");  // 수정 버튼 숨기기
	saveBtn.classList.remove("d-none");  // 저장 버튼 보이기
}

function updateMember() {
    let data = {
        empNo: document.getElementById("empNo").value, // 사번 추가
        empNm: document.getElementById("empNm").value,
        empPw: document.getElementById("empPw").value,
        empDept: document.getElementById("empDept").value,
        empPhone: document.getElementById("empPhone").value
    };

    fetch('/auth/update-user', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
    }).then(response => response.json())
      .then(result => {
          alert("회원 정보가 수정되었습니다!");

          // 세션 정보 갱신 후 새로고침
          fetch('/api/session')
              .then(response => response.json())
              .then(sessionData => {
                  location.reload();
              });
      }).catch(error => console.error("업데이트 실패:", error));
}
