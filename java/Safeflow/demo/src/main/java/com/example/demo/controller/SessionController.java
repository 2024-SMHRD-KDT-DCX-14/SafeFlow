package com.example.demo.controller;

import java.util.Collections;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Member;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api")
public class SessionController {

	@GetMapping("/session")
	@ResponseBody
	public ResponseEntity<?> getSessionInfo(HttpSession session) {
	    Member loggedInUser = (Member) session.getAttribute("loggedInUser");

	    if (loggedInUser == null) {
	        return ResponseEntity.status(401).body(Collections.singletonMap("message", "로그인이 필요합니다."));
	    }
	    
	    
	    return ResponseEntity.ok(Collections.singletonMap("data", loggedInUser));
	}

}

