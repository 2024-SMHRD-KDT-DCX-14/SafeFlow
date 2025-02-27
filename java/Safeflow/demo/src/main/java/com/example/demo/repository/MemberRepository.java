package com.example.demo.repository;

import com.example.demo.model.Member;

import java.math.BigDecimal;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, String> {

	// ✅ 사번(아이디) 중복 여부 확인
	@Query(value = "SELECT COUNT(*) FROM tb_employee WHERE emp_no = :empNo", nativeQuery = true)
	BigDecimal existsByEmpNo(@Param("empNo") String empNo);
    
    Optional<Member> findByEmpNo(String empNo);
    
}

