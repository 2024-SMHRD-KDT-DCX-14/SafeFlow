package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.CCTV;
import com.example.demo.model.Document;
import com.example.demo.model.Event;
import com.example.demo.repository.BoardRepository;
import com.example.demo.repository.EventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

	private final BoardRepository boardrepo;

	private final EventRepository eventrepo;

	// 최신 상위 5개
	public List<Document> getTop5Documents() {
		return boardrepo.findTop5ByCreatedAtDesc();
	};

	// bar 차트 그리기
	public List<Object[]> getRecentMonthlyEventCounts() {
		LocalDateTime endDate = LocalDateTime.now(); // 현재시간
		LocalDateTime startDate = endDate.minusMonths(6); // 6개월 전

		return eventrepo.findMonthlyEventCount(startDate, endDate);

	}

	// 무삭고 일수 계산
	public int getRecentAccidentDate() {

		LocalDateTime lastEventDateTime = eventrepo.findLatestCreatedAt();

		if (lastEventDateTime == null) {
			return 0; // 사건이 없으면 0일 반환
		}

		// LocalDateTime → LocalDate 변환 (날짜 차이 계산)
		// LocalDateTime에서 LocalDate로 변환 (시간 제외)
		LocalDate lastEventDate = lastEventDateTime.toLocalDate();
		LocalDate today = LocalDate.now();

		// 날짜 차이 계산 (정수형 반환)
		// long 타입이지만 (int)로 변환해서 반환
		// ChronoUnit.DAYS.between(시작날짜, 현재날짜)로 차이 계산
		return (int) ChronoUnit.DAYS.between(lastEventDate, today);

	}

	
	// 미확인 이벤트 리스트 반환
	public Page<Map<String, Object>> unconfirmedEventlist(Pageable pageable) {
	    // 완료되지 않은 이벤트 목록을 조회 (doneYn == 'N' 인 이벤트)
	    List<Event> allEvents = eventrepo.findByDoneYn(); 
	    List<Map<String, Object>> mappedEvents = new ArrayList<>();

	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	    
	    for (Event event : allEvents) {
	        Map<String, Object> eventMap = new HashMap<>();
	        
	        // 날짜 변환
	        String formattedDate = event.getCreatedAt().format(formatter);
	        
	        eventMap.put("eventIdx", event.getEventIdx());
	        eventMap.put("createdAt", formattedDate);
	        eventMap.put("eventType", event.getEventType());
	        eventMap.put("eventVideo", event.getEventVideo());
	        eventMap.put("doneYn", event.getDoneYn());
	        
	        // CCTV 엔티티에서 필요한 정보를 가져옴 (JOIN 효과)
	        CCTV cctv = event.getCctvIdx();
	        if (cctv != null) {
	            eventMap.put("cctvIdx", cctv.getId());           // CCTV의 기본키
	            eventMap.put("cctvCode", cctv.getCctvCode());       // CCTV 고유번호
	            eventMap.put("cctvLoc", cctv.getCctvLoc());         // 설치 장소
	        }
	        
	        mappedEvents.add(eventMap);
	    }

	    int start = Math.min((int) pageable.getOffset(), mappedEvents.size());
	    int end = Math.min((start + pageable.getPageSize()), mappedEvents.size());
	    
	    List<Map<String, Object>> pagedEvent = mappedEvents.subList(start, end);
	    
	    return new PageImpl<>(pagedEvent, pageable, mappedEvents.size());
	}

	
	// 이벤트 업데이트
	@Transactional
	public String updateEvent(Integer eventIdx, String content) {
		
		
		// eventIdx를 사용하여 해당 이벤트 찾기
		int updateRows = eventrepo.updateContentByEventIdx(eventIdx, content, 'Y');
		
		if(updateRows == 0 ) {
			return "이벤트를 찾을 수 없습니다.";
		}
		
		
		return "이벤트가 성공적으로 업데이트되었습니다.";
	}
	
	
}
