package com.example.demo.service;

import com.example.demo.config.VideoSaver;
import com.example.demo.model.CCTV;
import com.example.demo.model.Event;
import com.example.demo.repository.CCTVRepository;
import com.example.demo.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EventService {
    @Autowired
    private CCTVRepository cctvRepository;
    @Autowired
    private EventRepository eventRepository;

    private static final long COOLDOWN_TIME_MS = 30 * 1000;  // ✅ 30초로 증가
    private static final Map<String, Long> lastEventTimes = new HashMap<>(); // 🔹 CCTV별 이벤트별 Cooldown 적용

    public void processFrames(List<MultipartFile> frames, Long cctvIdx, String eventType, String detectionTime) {
        Optional<CCTV> cctvOptional = cctvRepository.findById(cctvIdx);

        if (cctvOptional.isEmpty()) {
            throw new RuntimeException("❌ CCTV 정보를 찾을 수 없습니다: " + cctvIdx);
        }

        long currentTime = System.currentTimeMillis();
        String eventKey = cctvIdx + "_" + eventType; // ✅ CCTV별 이벤트 유형별 Cooldown 적용

        if (lastEventTimes.containsKey(eventKey) &&
            (currentTime - lastEventTimes.get(eventKey)) < COOLDOWN_TIME_MS) {
            System.out.println("⏳ 이벤트 Cooldown 적용 중 (CCTV: " + cctvIdx + ", 이벤트: " + eventType + "), 저장하지 않음.");
            return; // ✅ 중복 저장 방지
        }

        lastEventTimes.put(eventKey, currentTime); // ✅ 마지막 이벤트 시간 업데이트

        CCTV cctv = cctvOptional.get();
        String videoFileName = "CCTV_" + cctvIdx + "_" + eventType + "_" + detectionTime + "/output.mp4";
        String videoFilePath = "C:/videos/" + videoFileName;

        try {
            // ✅ 이벤트 타입을 포함하여 저장
            VideoSaver.saveFramesToVideo(frames, cctvIdx.toString(), eventType, detectionTime);

            // ✅ 이벤트 정보 DB 저장
            Event event = new Event();
            event.setCctvIdx(cctv);
            event.setEventType(eventType);
            event.setEventVideo(videoFilePath);
            eventRepository.save(event);

            System.out.println("✅ 이벤트 저장 완료: " + videoFilePath);
        } catch (Exception e) {
            System.err.println("❌ 비디오 저장 실패: " + e.getMessage());
        }
    }
}
