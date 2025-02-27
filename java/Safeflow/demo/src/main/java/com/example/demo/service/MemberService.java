package com.example.demo.service;

import com.example.demo.model.Member;
import com.example.demo.repository.MemberRepository;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;

    // 회원가입
    public boolean registerUser(Member member) {
        try {
            memberRepository.save(member);
            return true;
        } catch (Exception e) {
            System.out.println("DB 저장 오류: " + e.getMessage());
            return false;
        }
    }
    
    
    // 로그인 요청 처리
    public Member login(String empNo, String empPw) {
        return memberRepository.findByEmpNo(empNo)
                .filter(member -> member.getEmpPw().equals(empPw))
                .orElse(null);
    }
    
    
    // ✅ 사번(아이디) 중복 여부 체크
    public BigDecimal isEmpNoExists(String empNo) {
        return memberRepository.existsByEmpNo(empNo);
    }
    
    
    @Transactional
    public Member updateMember(Member updatedMember, HttpSession session) {
        Member existingMember = memberRepository.findByEmpNo(updatedMember.getEmpNo()).orElse(null);

        if (existingMember != null) {
            existingMember.setEmpNm(updatedMember.getEmpNm());
            existingMember.setEmpPw(updatedMember.getEmpPw());
            existingMember.setEmpDept(updatedMember.getEmpDept());
            existingMember.setEmpPhone(updatedMember.getEmpPhone());
            Member savedMember = memberRepository.save(existingMember);
            
            session.setAttribute("loggedInUser", savedMember);
            return savedMember;
        }
        
        return null; // 회원이 없으면 null 반환
    }

}
