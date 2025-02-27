package com.example.demo.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Document;
import com.example.demo.model.Event;
import com.example.demo.service.DashboardService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

	private final DashboardService dashboardService;

	// ResponseEntity<> : postmapping시 반환 형태
	@GetMapping("/topboard")
	public List<Document> getDocutop5() {

		return dashboardService.getTop5Documents();
	}

	// bar 차트 만들기 6개월간 데이터 불러오기
	@PostMapping("/eventchart")
	public List<Object[]> eventchart() {

		return dashboardService.getRecentMonthlyEventCounts();
	}

	// 안전 사고일수 계산
	@GetMapping("/calculateNoAccident")
	public int calculateNoAccident() {

		return dashboardService.getRecentAccidentDate();
	}

	// 미완료 이벤트 불러오기
	@GetMapping("/unconfirmedEvent")
	public Page<Map<String, Object>> getunconfirmedEventlist(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size) {

		Pageable pageable = PageRequest.of(page, size);
		
		return dashboardService.unconfirmedEventlist(pageable);
	}
	
	
	// 미완료 이벤트 내용 작성
	@PostMapping("/uploadContent")
	public ResponseEntity<String> updateEvent(@RequestBody Map<String, Object> request){
		
		Integer eventIdx = Integer.valueOf(request.get("eventIdx").toString());
		String content = (String) request.get("content");
		
		// 서비스 호출하여 이벤트 업데이트
		String response = dashboardService.updateEvent(eventIdx, content);
		
		return ResponseEntity.ok(response);
	}
	
	@GetMapping("/video")
	public void getVideo(@RequestParam("filePath") String filePath, HttpServletResponse response) throws IOException {
	    // filePath 검증 및 허용된 경로인지 확인
	    File videoFile = new File(filePath);
	    if (!videoFile.exists()) {
	        response.sendError(HttpServletResponse.SC_NOT_FOUND);
	        return;
	    }

	    // MIME 타입 설정 (예: mp4 파일)
	    response.setContentType("video/mp4");

	    // 파일 데이터를 읽어 스트림으로 전송
	    try (InputStream is = new FileInputStream(videoFile);
	         OutputStream os = response.getOutputStream()) {
	        byte[] buffer = new byte[1024];
	        int bytesRead;
	        while ((bytesRead = is.read(buffer)) != -1) {
	            os.write(buffer, 0, bytesRead);
	        }
	        os.flush();
	    }
	}

	
}
