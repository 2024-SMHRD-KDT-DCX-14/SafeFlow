package com.example.demo.service;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.example.demo.model.Document;
import com.example.demo.repository.BoardRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class DocumentService {

	@Value("${save.path}")
	private String savePath;

	private final BoardRepository repo;

	// 전체 문서 조회 (paging)
	public Page<Map<String, Object>> getPagedDocuments(Pageable pageable) {

		// savePath 폴더 내의 모든 하위 디렉토리 가져오기
		File parentFolder = new File(savePath);

		if (!parentFolder.exists() || !parentFolder.isDirectory()) {
			return Page.empty();// Map 형태로 빈 값 반환
		}

		List<Map<String, Object>> documents = new ArrayList<>();

		// 모든 하위 디렉토리 목록 가져오기
		File[] categoryFolders = parentFolder.listFiles(File::isDirectory);

		if (categoryFolders != null) {
			// 각 카테고리에서 문서 목록을 가져와서 Map에 저장
			for (File categoryFolder : categoryFolders) {
				// 문서 유형에 맞는 파일 리스트
				List<String> files = getlistByCategory(categoryFolder.getName());

				for (String file : files) {
					Map<String, Object> categoryMap = new HashMap<>();

					categoryMap.put("category", categoryFolder.getName());
					categoryMap.put("documents", file);
					documents.add(categoryMap);
				}

			}
		}

		int start = (int) pageable.getOffset();
		int end = Math.min((start + pageable.getPageSize()), documents.size());

		List<Map<String, Object>> pagedDocs = documents.subList(start, end);

		return new PageImpl<>(pagedDocs, pageable, documents.size());
	}

	// 전체 문서 조회할 때, 문서 유형별 들어있는 리스트 가져오기 위함!
	public List<String> getlistByCategory(String categoryName) {

		File categoryFolder = new File(savePath + "/" + categoryName);

		// 폴더가 존재하지 않거나 파일이 없을 경우 빈 리스트 반환
		if (!categoryFolder.exists() || !categoryFolder.isDirectory()) {
			return Collections.emptyList();
		}

		// 폴더 내의 파일 리스트 가져오기
		String[] files = categoryFolder.list((dir, name) -> new File(dir, name).isFile());

		if (files == null) {
			return Collections.emptyList();
		} else {
			return Arrays.asList(files);
		}
	}

	// 카데고리별 문서 조회 (pageing)
	public Page<String> getPagingDoclistByCategory(String categoryName, int page, int size) {

		File categoryFolder = new File(savePath + "/" + categoryName);

		// 폴더가 존재하지 않거나 파일이 없을 경우 빈 페이지 반환
		if (!categoryFolder.exists() || !categoryFolder.isDirectory()) {
			return Page.empty();
		}

		// 폴더 내의 파일 리스트 가져오기
		String[] files = categoryFolder.list((dir, name) -> new File(dir, name).isFile());

		if (files == null || files.length == 0) {
			return Page.empty();
		}

		// 페이지네이션 처리
		Pageable pageable = PageRequest.of(page, size);
		int start = Math.min((int) pageable.getOffset(), files.length);
		int end = Math.min((start + pageable.getPageSize()), files.length);

		List<String> pagedFiles = Arrays.asList(files).subList(start, end);

		// 전체 파일 수 (files.length)와 페이지 정보를 이용하여 PageImpl 객체 생성
		return new PageImpl<>(pagedFiles, pageable, files.length);
	}

	// 문서 검색
	public Page<Map<String, Object>> search(String text, int page, int size) {

		List<Document> list = repo.findByDocuNmContaining(text);

		Pageable pageable = PageRequest.of(page, size);
		
		// 전체 리스트를 기준으로 페이징 범위 계산
		int start = Math.min((int) pageable.getOffset(), list.size());
		int end = Math.min((start + pageable.getPageSize()), list.size());
		// pageable.getOffset()는 현재 페이지에서 첫 번째 항목의 인덱스를 반환
		// list.size()보다 커지지 않도록 Math.min()을 사용하여 전체 리스트 범위를 넘지 않게 지정
		
		// 페이징된 결과
	    List<Document> pagedList = list.subList(start, end);
		
		// 페이지에 포함된 문서들을 순회하여 Map<String, Object> 형식으로 변환
		List<Map<String, Object>> result = new ArrayList<>();

		for (Document doc : pagedList) {
			Map<String, Object> documentMap = new HashMap<>();

			// 문서 명과 카테고리 정보를 Map에 추가
			documentMap.put("docuNm", doc.getDocuNm());
			documentMap.put("docuType", doc.getDocuType());

			result.add(documentMap);
		}

		// 페이지 객체 생성하여 반환
		return new PageImpl<>(result, pageable, list.size());
	}

	// 문서 PDF 불러오기
	public ResponseEntity<Resource> getFile(String category, String filename) {
		try {
			Path filePath = Paths.get(savePath, category, filename);
			Resource resource = new UrlResource(filePath.toUri());
			// UrlResource : URL을 기준으로 리소스를 읽어들임
			// toUri() : Path의 경로를 URI 객체로 변환 후 반환

			// 파일명 URL 인코딩 (UTF-8로 인코딩하고 공백은 %20으로 바꿔줌)
			String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString()).replaceAll("\\+",
					"%20");

			if (resource.exists() || resource.isReadable()) {
				// URL 인코딩된 파일명을 Content-Disposition 헤더에 추가
				return ResponseEntity.ok()
						.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedFilename + "\"")
						// HttpHeaders.CONTENT_DISPOSITION: 헤더는 클라이언트에게 해당 리소스를 어떻게 처리할지 알려주는 역할
						// inline: 파일을 웹 브라우저에서 inline(내장)으로 바로 열 수 있도록 하는 설정
						.body(resource);
			} else {
				return ResponseEntity.notFound().build(); // 404 반환
			}
		} catch (Exception e) {
			return ResponseEntity.internalServerError().build(); // 500반환
		}
	}

}
