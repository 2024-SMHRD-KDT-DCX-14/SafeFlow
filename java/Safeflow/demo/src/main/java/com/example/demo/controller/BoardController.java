package com.example.demo.controller;

import java.io.IOException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Document;
import com.example.demo.service.BoardService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class BoardController {
	
	private final RestTemplate restTemplate = new RestTemplate();
	private final String fastApiBaseUrl = "http://127.0.0.1:8001";
	private final BoardService service;
	
	// 파일 업로드전 파일먼저 가져옴
    @PostMapping("/uploadFile")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam() String doctype) {
    	
        try {
            // ✅ 서비스 호출하여 파일 저장
            Map<String, String> response = service.saveFile(file, doctype);
            return ResponseEntity.ok().body(response);
            
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "파일 업로드 실패", "message", e.getMessage()));
        }
    }
    
    
    // 파일 업로드 저장
	@PostMapping("/upload")
	public String upload(@RequestBody HashMap<String, Object> paramMap) {

		// document 객체의 값 확인
        System.out.println("제목: " + paramMap.get("title"));
        System.out.println("작성자: " + paramMap.get("writer"));
        System.out.println("날짜: " + paramMap.get("date"));
        System.out.println("파일경로: " + paramMap.get("fileUrl"));
        System.out.println("문서유형: " + paramMap.get("category"));
        
        // 변수로 저장
        String docuNm  = (String)paramMap.get("title"); // 문서명
        String empNo = (String)paramMap.get("writer"); // 등록자(사원번호)
        String dateStr = (String)paramMap.get("date"); // 해당 날짜 (승인 날짜)
        String docuFile = (String)paramMap.get("fileUrl"); // 문서 파일
        String docuType = (String)paramMap.get("category"); // 문서 유형
        
        // 날짜 변환 (YYYY-MM-DD → Date)
        Date date = null;
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            date = formatter.parse(dateStr);
        } catch (Exception e) {
            e.printStackTrace();
            return "error"; // 변환 실패 시 처리
        }
        
        // Document entitiy에 변수 저장
        Document doc = new Document();
        doc.setDocuNm(docuNm);
        doc.setEmpNo(empNo);
        doc.setDocuDate(date);
        doc.setDocuFile(docuFile);
        doc.setDocuType(docuType);
        
        service.saveDocument(doc);
        
        // FastAPI로 보낼 requset body 준비
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("pdf_directory", doc.getDocuFile());
        
        String fastApiUrl = fastApiBaseUrl + "/store";
        
        // HTTP 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // HTTP 요청 엔터티 생성
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // FastAPI 요청
        try {
            // FastAPI 요청
            ResponseEntity<Map> response = restTemplate.postForEntity(fastApiUrl, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("FastAPI 응답: " + response.getBody());
            } else {
                System.out.println("FastAPI 호출 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("FastAPI 서버 호출 중 오류 발생!");
        }
        
        return "redirect:/board";
	}
	
	
}