package com.example.demo.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Document;
import com.example.demo.repository.BoardRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class BoardService {

	private final BoardRepository boardRepository;

	@Value("${save.path}")
	private String savePath;

	@Value("${backup.path}")
	private String backupPath;

	// 파일 저장 메소드
	public Map<String, String> saveFile(MultipartFile file, String doctype) throws IOException {

		// (1) 문서 유형별 저장 경로 설정
	    String typeSavePath = Paths.get(savePath, doctype).toString();
	    String typeBackupPath = Paths.get(backupPath, doctype).toString();
		
		
		// (2) 저장 폴더 및 백업 폴더 생성
		// 저장 폴더 생성 확인
		File uploadDir = new File(typeSavePath);
		if (!uploadDir.exists()) {
			uploadDir.mkdirs();
		}

		// 백업 폴더 생성 확인
		File backupDir = new File(typeBackupPath);
		if (!backupDir.exists()) {
			backupDir.mkdirs();
		}

		// (3) 원본 파일명 유지
		String originalFileName = file.getOriginalFilename();
		Path filePath = Paths.get(typeSavePath, originalFileName);

		// (4) 기존 파일이 존재하면 백업
		if (Files.exists(filePath)) {

			// (3-1) 확장자 처리
			String extension = ""; // 확장자 담을 String 변수
			String backupFileName = ""; // originalFileName에서 파일명만 담을 변수

			// lastIndexOf()는 찾은 문자의 위치(인덱스)를 반환
			int dotIndex = originalFileName.lastIndexOf(".");

			// .을 찾을 수 없으면 -1을 반환
			if (dotIndex != -1) {
				// dotIndex부터 문자열 끝까지 잘라서 확장자를 추출 (.pdf, .jpg 등)
				extension = originalFileName.substring(dotIndex);
				backupFileName = originalFileName.substring(0, dotIndex); // 확장자 제외한 파일명
			}

			// (3-2) 파일명에 현재 시간날짜 추가 + 확장자
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
			String final_backupFileName = backupFileName + "_" + timestamp + extension; // 확장자 유지

			Path backupFilePath = Paths.get(typeBackupPath, final_backupFileName);

			// 기존 파일을 백업 폴더로 이동
			Files.move(filePath, backupFilePath, StandardCopyOption.REPLACE_EXISTING);
			System.out.println("기존 파일 백업됨: " + backupFilePath.toString());
		}

		// (4) 새로운 파일 저장
		Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
		// file.getInputStream() → 업로드된 파일의 데이터를 읽음
		// filePath: 저장 경로
		// StandardCopyOption.REPLACE_EXISTING -> 동일한 경우 덮어쓰기, 아마 없을 듯
		System.out.println("새 파일 저장됨: " + filePath.toString());

		// (5) 파일 접근 URL 생성
		// 클라이언트가 접근할 수 있도록 URL 형식으로 작성
		String fileUrl = "C:/uploadFile/" + doctype + "/" + originalFileName; // 예제 URL (파일 서버 경로 필요)

		return Map.of("fileUrl", fileUrl);

	}

	
	// 문서 조회하고 있으면 업데이트로 들어가
	public void saveDocument(Document doc) {
		
		// 문서명이 동일한 기존 데이터 조회
        Optional<Document> existingDoc = boardRepository.findByDocuNm(doc.getDocuNm());

        if (existingDoc.isPresent()) {
            // 기존 문서가 있으면 업데이트 수행
            Document updateDoc = existingDoc.get();
            updateDoc.setDocuFile(doc.getDocuFile());
            updateDoc.setDocuDate(doc.getDocuDate());
            updateDoc.setEmpNo(doc.getEmpNo());
            
            boardRepository.save(updateDoc); // UPDATE 실행
            
        } else {
            // 기존 문서가 없으면 새로 추가 (INSERT)
        	boardRepository.save(doc);
        }
	}
}
