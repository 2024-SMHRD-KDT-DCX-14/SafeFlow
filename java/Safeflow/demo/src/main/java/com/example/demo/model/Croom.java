package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tb_croom")
public class Croom {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "croom_idx")
    private Integer croomIdx; // 채팅방 ID

    @Column(name = "emp_no", nullable = false)
    private String empNo; // 생성자 (사용자)

    // DB에서 기본값을 자동으로 설정하므로 애플리케이션에서 값을 설정할 필요 없음
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt; // 생성 시간
}
