package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/cctv")
public class EventController {
    
    @Autowired
    private EventService eventService;

    @PostMapping(value = "/event", consumes = "multipart/form-data")
    public ResponseEntity<?> receiveEvent(
            @RequestParam("cctv_idx") Long cctv_idx, 
            @RequestParam("eventType") String eventType, 
            @RequestParam("detectionTime") String detectionTime,
            @RequestPart(value = "frames", required = false) List<MultipartFile> frames) {  // ✅ @RequestPart 사용

        System.out.println("📡 [Java] 이벤트 수신: CCTV " + cctv_idx + ", 이벤트 " + eventType);
        System.out.println("📸 [Java] 받은 프레임 개수: " + (frames != null ? frames.size() : 0));

        if (frames == null || frames.isEmpty()) {
            System.out.println("⚠ 프레임이 없음, 이벤트를 저장하지 않음.");
            return ResponseEntity.status(400).body("프레임 데이터가 없습니다.");
        }

        try {
            eventService.processFrames(frames, cctv_idx, eventType, detectionTime);
            return ResponseEntity.ok("✅ 이벤트 처리 완료!");
        } catch (Exception e) {
            System.err.println("❌ 이벤트 처리 실패: " + e.getMessage());
            return ResponseEntity.status(500).body("이벤트 처리 중 오류 발생.");
        }
    }

}

