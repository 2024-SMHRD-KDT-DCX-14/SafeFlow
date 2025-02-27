package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "TB_CCTV")
public class CCTV {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CCTV_IDX")
	private Long id;

	@Column(name = "CCTV_CODE", nullable = false, unique = true)
	private String cctvCode;

	@Column(name = "CCTV_LOC", nullable = false)
	private String cctvLoc;

	@Column(name = "INSTALLED_AT", nullable = false, updatable = false)
	private LocalDateTime installedAt = LocalDateTime.now();

	@Column(name = "EMP_NO", nullable = false)
	private String empNo;

	public CCTV(String cctvCode, String empDept, String empNo) {
		this.cctvCode = cctvCode;
		this.cctvLoc = empDept; // 만약 empDept가 CCTV 위치라면
		this.empNo = empNo;
		this.installedAt = LocalDateTime.now();
	}
}