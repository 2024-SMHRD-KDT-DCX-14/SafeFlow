package com.example.demo.service;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.model.Croom;
import com.example.demo.repository.ChatRoomRepository;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ChatRoomService {

    private final JdbcTemplate jdbcTemplate;
    
    private final ChatRoomRepository chatRoomRepository;
    
    public ChatRoomService(JdbcTemplate jdbcTemplate, ChatRoomRepository chatRoomRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.chatRoomRepository = chatRoomRepository;
    }

    // 사용자 참여 중인 채팅방 조회
    public List<Map<String, Object>> getUserChatRooms(String empNo) {
        String sql = "SELECT * FROM TB_CROOM WHERE EMP_NO = ?";
        return jdbcTemplate.queryForList(sql, empNo);
    }
    
    public Croom createChatRoom(Croom croom) {
        return chatRoomRepository.save(croom);
    }
    
    @Modifying 
    @Transactional  
    public void deleteChatRoom(Integer CroomIdx) {
    	chatRoomRepository.deleteByCroomIdx(CroomIdx);
    }
}
