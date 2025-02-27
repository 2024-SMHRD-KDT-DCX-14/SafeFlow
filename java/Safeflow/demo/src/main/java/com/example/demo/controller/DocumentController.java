package com.example.demo.controller;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Document;
import com.example.demo.service.DocumentService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

	private final DocumentService service;	
	
	// 검색 API
    @GetMapping("/search")
    public Page<Map<String, Object>> search(@RequestParam("text") String text,
    		@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
     
      
        return service.search(text, page, size);
    }
	
	
	// 전체 문서 조회
	@GetMapping("/all")
	public ResponseEntity<Page<Map<String, Object>>> getAllDocuments(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size
			){
		
		Pageable pageable = PageRequest.of(page, size, Sort.by("category").ascending());
		Page<Map<String, Object>> documentsPage = service.getPagedDocuments(pageable);
		
		return ResponseEntity.ok(documentsPage);
	}
	
	
	// 카테고리에 맞는 문서 목록을 반환
    @GetMapping("/category/{categoryName}")
    public Page<String> getDocumentsByCategory(@PathVariable String categoryName,
    		@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
        
    	return service.getPagingDoclistByCategory(categoryName, page, size);
    }

    // 문서명을 클릭하면 PDF를 불러오기 함.
    @GetMapping("/{category}/{filename}")
    public ResponseEntity<Resource> getFile(@PathVariable String category, @PathVariable String filename) {
        
        try {
            // 한글 카테고리 & 파일명 URL 디코딩
            String decodedCategory = URLDecoder.decode(category, StandardCharsets.UTF_8.name());
            String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8.name());

            return service.getFile(decodedCategory, decodedFilename);
            
        } catch (UnsupportedEncodingException e) {
            return ResponseEntity.badRequest().build();
        }
    	
    }
    

    
    
}
