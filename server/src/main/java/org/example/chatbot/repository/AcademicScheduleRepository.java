package org.example.chatbot.repository;

import org.example.chatbot.domain.AcademicSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicScheduleRepository extends JpaRepository<AcademicSchedule, Long> {

    List<AcademicSchedule> findByDateContainingAndContentContaining(String date, String keyword);

    List<AcademicSchedule> findByDateContaining(String date);

    List<AcademicSchedule> findByContentContaining(String keyword);

    List<AcademicSchedule> findAll();
}