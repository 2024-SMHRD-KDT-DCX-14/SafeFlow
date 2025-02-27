package com.example.demo.service;

import com.example.demo.model.CCTV;
import com.example.demo.repository.CCTVRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CCTVService {

    @Autowired
    private CCTVRepository cctvRepository; // CCTV 엔티티를 관리하는 JPA Repository

    // ✅ CCTV 데이터 저장 메서드 추가
    public void saveCCTV(CCTV cctv) {
        cctvRepository.save(cctv);
    }

    // ✅ 특정 부서의 CCTV 리스트 조회
    public List<CCTV> getCCTVsByEmoNo(String empDept) {
        return cctvRepository.findByEmpNo(empDept);
    }
    
    public Optional<CCTV> findByCctvCodeAndCctvLocAndEmpNo(String cctvCode, String cctvLoc, String empNo) {
        return cctvRepository.findByCctvCodeAndCctvLocAndEmpNo(cctvCode, cctvLoc, empNo);
    }
}
