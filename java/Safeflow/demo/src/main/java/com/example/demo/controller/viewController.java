package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class viewController {

	@GetMapping("/")
	public String firstPage() {
		
		return "firstPage";
	}
	
	@PostMapping("/login")
	public String login() {
		
		return "page";
	}
	
}
