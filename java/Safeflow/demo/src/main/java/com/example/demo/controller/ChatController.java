package com.example.demo.controller;

import com.example.demo.model.Croom;
import com.example.demo.model.Message;
import com.example.demo.service.ChatRoomService;
import com.example.demo.service.ChatService;
import com.example.demo.service.MessageService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
	
	private final RestTemplate restTemplate = new RestTemplate();
    private final ChatService chatService;
    private final ChatRoomService chatRoomService;
    private final MessageService messageService;
    private final String fastApiBaseUrl = "http://127.0.0.1:8000/chat";

    public ChatController(ChatService chatService, MessageService messageService, ChatRoomService chatRoomService) {
        this.chatService = chatService;
        this.messageService = messageService;
        this.chatRoomService = chatRoomService;
    }

    @GetMapping("/rooms/{empNo}")
    public ResponseEntity<List<Croom>> getChatRooms(@PathVariable String empNo) {
        List<Croom> chatRooms = chatService.getChatRooms(empNo);
        return ResponseEntity.ok(chatRooms);
    }

    @PostMapping("/createRooms/{empNo}")
    public ResponseEntity<?> createChatRoom(@PathVariable("empNo") String empNo) {
    	System.out.println("Received empNo: " + empNo); // 로그 추가
        if (empNo == null || empNo.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("empNo는 필수 입력 값입니다.");
        }

        try {
            Croom newRoom = new Croom();
            newRoom.setEmpNo(empNo);
            return ResponseEntity.ok(chatRoomService.createChatRoom(newRoom));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("채팅방 생성 중 오류 발생");
        }
    }
    
    @GetMapping("/messages/{croomIdx}")
    public ResponseEntity<?> getMessages(@PathVariable Integer croomIdx) {
        System.out.println("📢 요청 받은 croomIdx: " + croomIdx);
        try {
            List<Message> messages = messageService.getMessages(croomIdx);
            System.out.println("📩 메시지 리스트: " + messages);
            
            // 메시지가 없을 경우에도 200 OK 반환
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("메시지 로딩 중 오류 발생");
        }
    }
    
    @PostMapping("/messages")
    public ResponseEntity<?> sendMessageToFastAPI(@RequestBody Message message) {
        try {
            System.out.println("📥 요청 받은 데이터: " + message);

            if (message.getCreatedAt() == null) {
                message.setCreatedAt(LocalDateTime.now());
            }

            // FastAPI로 보낼 데이터 준비
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", message.getChat());
            requestBody.put("croomIdx", message.getCroomIdx());
            requestBody.put("chatter", message.getChatter());
            requestBody.put("ratings", message.getRatings());
            requestBody.put("createdAt", message.getCreatedAt().toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // FastAPI 요청 (응답까지 기다림)
            ResponseEntity<Map> response = restTemplate.postForEntity(fastApiBaseUrl, entity, Map.class);

            // FastAPI 응답 확인
            Map<String, Object> responseBody = response.getBody();
            System.out.println("📤 FastAPI 응답: " + responseBody);

            // 응답 메시지를 DB에 저장 (요청 데이터를 먼저 저장)
            Message requestMessage = new Message();
            requestMessage.setCroomIdx(message.getCroomIdx());
            requestMessage.setRatings(message.getRatings());
            requestMessage.setChatter(message.getChatter());
            requestMessage.setChat(message.getChat());  // 요청한 데이터 저장
            requestMessage.setCreatedAt(message.getCreatedAt());

            messageService.saveMessage(requestMessage); // DB에 요청 데이터 저장

            // FastAPI 응답에서 'response'와 'documentTitle' 객체를 직접 처리
            String responseContent = (String) responseBody.get("response");  // 응답을 String으로 처리

            // 응답을 DB에 저장
            Message responseMessage = new Message();
            responseMessage.setCroomIdx(message.getCroomIdx());
            responseMessage.setRatings("A");
            responseMessage.setChatter("ChatBot");

            // 응답 메시지와 문서 제목을 저장
            responseMessage.setChat("\n" + responseContent);  // 제목 + 응답 메시지 저장
            responseMessage.setCreatedAt(LocalDateTime.now());

            messageService.saveMessage(responseMessage); // DB에 응답 메시지 저장

            return ResponseEntity.ok(responseMessage);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("FastAPI와 통신 중 오류 발생");
        }
    }
    
    @DeleteMapping("/rooms/{croomIdx}")
    public ResponseEntity<?> deleteChatRoom(@PathVariable Integer croomIdx) {
        try {
            // 채팅방에 속한 메시지들 삭제
            messageService.deleteMessagesByRoomId(croomIdx);

            // 채팅방 삭제
            chatRoomService.deleteChatRoom(croomIdx);

            return ResponseEntity.ok("채팅방과 메시지가 삭제되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("채팅방 삭제 중 오류 발생");
        }
    }
    
 // ✅ 별점 저장 API
    @PostMapping("/rating")
    public ResponseEntity<?> saveRating(@RequestBody Map<String, Object> request) {
        try {
            int messageId = Integer.parseInt(request.get("messageId").toString());
            int ratingValue = Integer.parseInt(request.get("rating").toString());

            Optional<Message> optionalMessage = messageService.findById(messageId);
            if (optionalMessage.isPresent()) {
                Message message = optionalMessage.get();
                message.setRatings(convertRatingToGrade(ratingValue));
                messageService.saveMessage(message);
                return ResponseEntity.ok("✅ 별점 저장 성공");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ 메시지를 찾을 수 없음");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ 서버 오류 발생");
        }
    }

    // ✅ 별점을 A~E 등급으로 변환
    private String convertRatingToGrade(int rating) {
        switch (rating) {
            case 5: return "A";
            case 4: return "B";
            case 3: return "C";
            case 2: return "D";
            case 1: return "E";
            default: return null;
        }
    }

}
