package com.example.demo.model;

import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "TB_DOCUMENT")
public class Document {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DOCU_IDX")
	private Integer docuIdx;
	
	@Column(name = "DOCU_NM") // 파일명
	private String docuNm;
	
	@Column(name = "DOCU_FILE") // 파일 저장 경
	private String docuFile;
	
	@Column(name = "DOCU_TYPE") // 문서 유형
	private String docuType;
	
	@Column(name = "DOCU_DATE")
	private Date docuDate;

	@Column(name = "EMP_NO")
	private String empNo;
	
	@CreationTimestamp
	@Temporal(TemporalType.DATE)
	@Column(updatable=false, name="CREATED_AT")
	private Date createdAt;

}
