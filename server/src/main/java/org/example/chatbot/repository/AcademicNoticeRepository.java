package org.example.chatbot.repository;

import org.example.chatbot.domain.AcademicNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicNoticeRepository extends JpaRepository<AcademicNotice, Long> {

    /**
     * 제목과 날짜를 기준으로 공지사항을 검색합니다.
     *
     * @param noticeDate 공지 날짜 (부분 일치)
     * @param title 공지 제목 (부분 일치)
     * @return 검색된 공지 목록
     */
    List<AcademicNotice> findByNoticeDateContainingAndTitleContaining(String noticeDate, String title);

    /**
     * 날짜를 기준으로 공지사항을 검색합니다.
     *
     * @param noticeDate 공지 날짜 (부분 일치)
     * @return 검색된 공지 목록
     */
    List<AcademicNotice> findByNoticeDateContaining(String noticeDate);

    /**
     * 제목을 기준으로 공지사항을 검색합니다.
     *
     * @param title 공지 제목 (부분 일치)
     * @return 검색된 공지 목록
     */
    List<AcademicNotice> findByTitleContaining(String title);

    /**
     * 전체 공지사항 목록을 조회합니다.
     *
     * @return 전체 공지 목록
     */
    List<AcademicNotice> findAll();

}