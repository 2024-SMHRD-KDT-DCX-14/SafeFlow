package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.sql.Timestamp;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "TB_EMPLOYEE")
public class Member {
    @Id
    @Column(name = "EMP_NO", nullable = false)
    private String empNo;

    @Column(name = "EMP_PW", nullable = false)
    private String empPw;

    @Column(name = "EMP_NM", nullable = false)
    private String empNm;

    @Column(name = "EMP_DEPT")
    private String empDept;

    @Column(name = "EMP_ROLE")
    private String empRole = "USER";

    @Column(name = "EMP_PHONE")
    private String empPhone;
	
    @Column(name = "JOINED_AT", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", updatable = false)
    private Timestamp joinedAt;

}
