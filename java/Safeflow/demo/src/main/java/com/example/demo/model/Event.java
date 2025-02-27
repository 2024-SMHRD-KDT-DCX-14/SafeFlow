	package com.example.demo.model;
	
	import jakarta.persistence.*;
	import lombok.Getter;
	import lombok.NoArgsConstructor;
	import lombok.Setter;
	import java.time.LocalDateTime;
	
	@Entity
	@Getter
	@Setter
	@NoArgsConstructor
	@Table(name = "TB_EVENT")
	public class Event {
	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    @Column(name = "EVENT_IDX")
	    private Long eventIdx;
	
	    @ManyToOne
	    @JoinColumn(name = "CCTV_IDX", nullable = false)
	    private CCTV cctvIdx;
	
	    @Column(name = "EVENT_TYPE", nullable = false, length = 50)
	    private String eventType;
	
	    @Lob
	    @Column(name = "EVENT_CONTENT")
	    private String eventContent = "작성이 필요합니다";
	
	    @Column(name = "EVENT_VIDEO", length = 1000)
	    private String eventVideo;
	
	
	    @Column(name = "DONE_YN", nullable = false, length = 1)
	    private char doneYn = 'N';
	
	    @Column(name = "CREATED_AT", nullable = false, updatable = false)
	    private LocalDateTime createdAt = LocalDateTime.now();
	
	    public Event(CCTV cctv, String eventType, String eventVideo) {
	        this.cctvIdx = cctv;
	        this.eventType = eventType;
	        this.eventVideo = eventVideo;
	        this.doneYn = 'N';
	        this.createdAt = LocalDateTime.now();
	    }
	}