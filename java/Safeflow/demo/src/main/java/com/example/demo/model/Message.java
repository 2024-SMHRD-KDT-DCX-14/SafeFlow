package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Getter
@Setter
@Table(name = "TB_CHAT")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CHAT_IDX")
    private Integer chatIdx;

    @Column(name = "CROOM_IDX", nullable = false)
    private Integer croomIdx;

    @Column(name = "CHATTER", nullable = false, length = 20)
    private String chatter;

    @Lob
    @Column(name = "CHAT", nullable = false)
    private String chat;

    @Column(name = "RATINGS", length = 1)
    private String ratings = "A";

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}