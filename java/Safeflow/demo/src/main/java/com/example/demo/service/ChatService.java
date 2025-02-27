package com.example.demo.service;

import com.example.demo.model.Croom;
import com.example.demo.repository.ChatRoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;

    public ChatService(ChatRoomRepository chatRoomRepository) {
        this.chatRoomRepository = chatRoomRepository;
    }

    // 특정 empNo의 채팅방 목록 조회
    public List<Croom> getChatRooms(String empNo) {
        return chatRoomRepository.findByEmpNo(empNo);
    }

    public boolean checkEmpNoExists(String empNo) {
        int count = chatRoomRepository.countByEmpNo(empNo);
        return count > 0;
    }

}

