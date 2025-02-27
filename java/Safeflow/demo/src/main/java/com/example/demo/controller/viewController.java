package com.example.demo.controller;

import com.example.demo.model.Member;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class viewController {

    @GetMapping("/")  // ✅ 메인 페이지에서 로그인 처리
    public String showHomePage(Model model) {
        model.addAttribute("member", new Member());  // ✅ 빈 DTO 객체 추가
        return "visit"; // resources/templates/visit.html
    }

    @GetMapping("/auth/register")  // ✅ 회원가입 페이지
    public String showRegisterPage(Model model) {
        model.addAttribute("member", new Member());  // ✅ 빈 DTO 객체 추가
        return "register"; // resources/templates/register.html
    }

    @GetMapping("/page")  // ✅ 로그인 후 이동할 페이지
    public String showPage() {
        return "page"; // resources/templates/page.html
    }
    
    @GetMapping("/dashboard") //대시보드 띄워주기
    public String dashboard() {
        return "dashboard"; // templates/dashboard.html을 렌더링
    }
    @GetMapping("/cctv") //대시보드 띄워주기
    public String cctv() {
        return "cctv"; // templates/dashboard.html을 렌더링
    }
    
    @GetMapping("/board")
    public String board() {
    	return "board"; // templates/board.html을 렌더링
    }
    
    @GetMapping("/myPage")
    public String myPage() {
    	return "mypage"; // templates/board.html을 렌더링
    }
    
}
