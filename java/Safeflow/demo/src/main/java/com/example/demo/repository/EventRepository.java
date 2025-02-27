package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Event;

//CCTVLogRepository.java
@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {

	/**
	 * 특정 시간 이후 발생한 이상 탐지 로그 조회
	 * 
	 * @param timestamp - 기준 시간
	 * @return List<CCTVLog> - 해당 시간 이후의 이상 탐지 로그 목록
	 */
	List<Event> findByCreatedAt(LocalDateTime timestamp);

	/**
	 * 특정 이벤트 내용으로 필터링된 로그 조회
	 * 
	 * @param event - 이벤트 내용
	 * @return List<CCTVLog> - 해당 이벤트 로그 목록
	 */
	List<Event> findByEventType(String event);

	// 최근 6개월 데이터 bar chart 그리기
	@Query(value = "SELECT " + "EXTRACT(YEAR FROM e.CREATED_AT) AS year, "
			+ "EXTRACT(MONTH FROM e.CREATED_AT) AS month, " + "COUNT(*) AS eventCount " + "FROM tb_event e "
			+ "WHERE e.CREATED_AT BETWEEN :startDate AND :endDate "
			+ "GROUP BY EXTRACT(YEAR FROM e.CREATED_AT), EXTRACT(MONTH FROM e.CREATED_AT) "
			+ "ORDER BY year, month", nativeQuery = true)
	List<Object[]> findMonthlyEventCount(@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate);

	// 가장 최근 발생한 이벤트 시간
	@Query(value = """
			SELECT CREATED_AT
			FROM (SELECT CREATED_AT FROM TB_EVENT ORDER BY CREATED_AT DESC)
			WHERE ROWNUM = 1
			""", nativeQuery = true)
	LocalDateTime findLatestCreatedAt();

	// 미확인("N") 이벤트리스트 불러오기
	@Query(value = """
			SELECT * FROM TB_EVENT WHERE DONE_YN='N' ORDER BY CREATED_AT
			""", nativeQuery = true)
	List<Event> findByDoneYn();

	// 이벤트의 content만 업데이트하는 쿼리 메서드 추가
	@Modifying
	@Query("UPDATE Event e SET e.eventContent = :content, e.doneYn = :doneYN WHERE e.eventIdx = :eventIdx")
	int updateContentByEventIdx(@Param("eventIdx") Integer eventIdx, @Param("content") String content, char doneYN);

}
