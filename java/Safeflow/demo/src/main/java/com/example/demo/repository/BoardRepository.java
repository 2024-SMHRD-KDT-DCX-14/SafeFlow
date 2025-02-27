package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Document;

@Repository
public interface BoardRepository extends JpaRepository<Document, Integer> {

	// 문서명이 동일한 기존 데이터 조회
	public Optional<Document> findByDocuNm(String docuNm); // 문서명으로 조회
	
	// 검색시 문서명에 따라 조회됨.
	public List<Document> findByDocuNmContaining(String text); // Containing: like %text%와 동일하게 기능을함
	
	// 날짜 기준으로 상위 5개 문서 조회 (최신 순)
	@Query("SELECT d FROM Document d WHERE ROWNUM <= 5 ORDER BY d.createdAt DESC")
	List<Document> findTop5ByCreatedAtDesc();
}
