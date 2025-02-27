package com.example.demo.controller;

import com.example.demo.model.Member;
import com.example.demo.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/auth")
public class memberController {

    @Autowired
    private MemberService memberService;

 // ✅ 회원가입 요청 처리 (리다이렉트 + alert 추가)
 	@PostMapping("/register")
 	public String registerUser(@ModelAttribute Member member, RedirectAttributes redirectAttributes) {
 		Member newMember = new Member(member.getEmpNo(), member.getEmpPw(), member.getEmpNm(), member.getEmpDept(),
 				member.getEmpRole(), member.getEmpPhone(), null);

 		boolean success = memberService.registerUser(newMember);

 		if (success) {
 			redirectAttributes.addFlashAttribute("message", "회원가입 성공!");
 			return "redirect:/auth/register";
 		} else {
 			redirectAttributes.addFlashAttribute("error", "회원가입 실패: 이미 존재하는 아이디입니다.");
 			return "redirect:/auth/register";
 		}
 	}
    
    /**
     * ✅ 로그인 요청 처리
     */
    @PostMapping("/login")
    public String login(@ModelAttribute Member member, HttpSession session, RedirectAttributes redirectAttributes) {
        Member user = memberService.login(member.getEmpNo(), member.getEmpPw());

        if (user != null) {
            session.setAttribute("loggedInUser", user);
            session.setAttribute("EMP_NO", user.getEmpNo());  // ✅ EMP_NO도 세션에 저장
            session.setAttribute("EMP_DEPT", user.getEmpDept());
            return "redirect:/page"; // ✅ 로그인 성공 후 page.html로 이동
        } else {
        	redirectAttributes.addFlashAttribute("error", "로그인 실패: 사번 또는 비밀번호가 올바르지 않습니다.");
            return "redirect:/";

        }
    }

    /**
     * ✅ 로그인된 사용자 정보 확인 API
     */
    @GetMapping("/session")
    @ResponseBody
    public ResponseEntity<?> getSessionInfo(HttpSession session) {
        Member loggedInUser = (Member) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("message", "로그인이 필요합니다."));
        }

        return ResponseEntity.ok(loggedInUser);
    }

    /**
     * ✅ 로그아웃 요청 처리
     */
 // ✅ 로그아웃 처리
 	@GetMapping("/logout")
 	public String logout(HttpSession session) {
 		session.invalidate();
 		return "redirect:/";
 	}

 	// 사번 중복 체크
 	// ✅ 아이디 중복 체크 API
 	@GetMapping("/checkEmpNo")
 	public ResponseEntity<Map<String, Object>> checkEmpNo(@RequestParam String empNo) {
 		
 		Map<String, Object> response = new HashMap();
 		
 		BigDecimal exists = memberService.isEmpNoExists(empNo); // 중복 체크 로직

 		// BigDecimal을 boolean으로 변환 (exists 값이 0이 아닌 값일 경우 true)
 		boolean isExists = exists.compareTo(BigDecimal.ZERO) !=0;
 		
 		if (isExists) {
 			response.put("exists", true);
 			response.put("message", "이미 존재하는 아이디입니다.");
 		} else {
 			response.put("exists", false);
 			response.put("message", "사용 가능한 아이디입니다.");
 		}
 		return ResponseEntity.ok(response);
 	}
 	
 	@PostMapping("/update-user")
 	public ResponseEntity<?> updateUser(@RequestBody Member updatedMember, HttpSession session) {
 	    System.out.print("🔹 회원 정보 업데이트 요청: {}"+ updatedMember);

 	    Member updatedUser = memberService.updateMember(updatedMember, session);

 	    if (updatedUser == null) {
 	    	System.out.print("⚠ 회원 정보 수정 실패: 해당 회원을 찾을 수 없음 (empNo={})"+ updatedMember.getEmpNo());
 	        return ResponseEntity.status(404).body(Collections.singletonMap("message", "회원 정보를 찾을 수 없습니다."));
 	    }

 	   System.out.print("✅ 회원 정보 수정 성공: {}"+ updatedUser);
 	    return ResponseEntity.ok(Collections.singletonMap("message", "회원 정보 수정 완료"));
 	}


}
