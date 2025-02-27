package com.example.demo.service;

import com.example.demo.model.Message;
import com.example.demo.repository.MessageRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    // 특정 empNo의 채팅방 목록 조회
    public List<Message> getMessages(Integer croomIdx) {
        return messageRepository.findByCroomIdx(croomIdx);
    }
    
    // 메시지 저장
    public Message saveMessage(Message message) {
        return messageRepository.save(message);
    }	
    
    // 채팅방에 속한 메시지 삭제
    public void deleteMessagesByRoomId(Integer croomIdx) {
        messageRepository.deleteByCroomIdx(croomIdx);
    }
    
    public Optional<Message> findById(Integer messageId){
    	return messageRepository.findById(messageId);
    }
}

