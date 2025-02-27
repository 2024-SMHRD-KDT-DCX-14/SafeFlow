package com.example.demo.controller;

import com.example.demo.model.CCTV;
import com.example.demo.repository.CCTVRepository;
import com.example.demo.service.CCTVService;
import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cctv")
public class CCTVController {

    @Autowired
    private CCTVService cctvService;
    
    @Autowired
    private CCTVRepository cctvRepository;

    @PostMapping("/register")
    public ResponseEntity<?> registerCCTV(@RequestBody Map<String, Object> request, HttpSession session) {
        String empNo = ((String) session.getAttribute("EMP_NO")).trim();
        String empDept = (String) session.getAttribute("EMP_DEPT");

        if (empNo == null || empDept == null) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "로그인 정보가 없습니다."));
        }

        // 요청에서 cctvCodes 가져오기 (제네릭 타입 명확하게 처리)
        Object cctvCodesObj = request.get("cctvCodes");
        if (!(cctvCodesObj instanceof List)) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "CCTV Code가 비어 있습니다."));
        }
        @SuppressWarnings("unchecked")
        List<String> cctvCodes = (List<String>) cctvCodesObj;
        if (cctvCodes.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "최소 1개 이상의 CCTV Code가 필요합니다."));
        }

        System.out.println("📌 CCTV 등록 요청 - EMP_NO: " + empNo + ", EMP_DEPT: " + empDept);
        System.out.println("📌 CCTV 코드 리스트: " + cctvCodes);

        // 해당 사용자의 CCTV를 등록 (동일 CCTV_CODE는 EMP_NO별로 별도 등록)
        for (String cctvCode : cctvCodes) {
            CCTV cctv = new CCTV(cctvCode, empDept, empNo);
            cctvService.saveCCTV(cctv);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "✅ " + cctvCodes.size() + "개의 CCTV 등록 완료");
        response.put("cctvCodes", cctvCodes);
        return ResponseEntity.ok(response);
    }
    
    
    @GetMapping("/list")
    public List<Map<String, Object>> getCCTVsByEmployee(HttpSession session) {
        String empNo = ((String) session.getAttribute("EMP_NO")).trim();
        System.out.println(empNo);
        if(empNo == null) {
            return Collections.emptyList();
        }
        List<CCTV> cctvs = cctvRepository.findByEmpNo(empNo);
        
        System.out.println(cctvs);
        List<Map<String, Object>> response = new ArrayList<>();

        for (CCTV cctv : cctvs) {
            Map<String, Object> cctvData = new HashMap<>();
            cctvData.put("cctvIdx", cctv.getId());  // 엔티티의 id를 cctvIdx로 사용
            cctvData.put("cctvCode", cctv.getCctvCode());
            cctvData.put("cctvLoc", cctv.getCctvLoc());
            response.add(cctvData);
        }
        return response;
    }

}
