package com.example.demo.repository;

import com.example.demo.model.CCTV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CCTVRepository extends JpaRepository<CCTV, Long> {
	List<CCTV> findByEmpNo(String empNo);

	Optional<CCTV> findByCctvCodeAndCctvLocAndEmpNo(String cctvCode, String cctvLoc, String empNo);

}