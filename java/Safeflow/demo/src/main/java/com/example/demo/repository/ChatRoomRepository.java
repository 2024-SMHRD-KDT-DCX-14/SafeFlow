package com.example.demo.repository;

import com.example.demo.model.Croom;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomRepository extends JpaRepository<Croom, Long> {
    
    List<Croom> findByEmpNo(String empNo);
    
    @Query("SELECT COUNT(c) FROM Croom c WHERE c.empNo = :empNo")
    int countByEmpNo(@Param("empNo") String empNo);
    
    void deleteByCroomIdx(Integer croomIdx);

}
