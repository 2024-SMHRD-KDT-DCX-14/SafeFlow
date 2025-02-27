package com.example.demo.repository;

import com.example.demo.model.Message;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Integer> {
	List<Message> findByCroomIdx(Integer croomIdx);
	
	@Modifying
    @Transactional
	void deleteByCroomIdx(Integer croomIdx);
	
	Optional<Message> findById(Integer id);
}
