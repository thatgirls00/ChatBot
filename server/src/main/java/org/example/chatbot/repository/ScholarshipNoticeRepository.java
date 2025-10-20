package org.example.chatbot.repository;

import org.example.chatbot.domain.ScholarshipNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScholarshipNoticeRepository extends JpaRepository<ScholarshipNotice, Long> {

    /**
     * 날짜와 제목을 기준으로 장학 공지사항을 검색합니다.
     *
     * @param date  날짜 (부분 일치)
     * @param title 제목 키워드 (부분 일치)
     * @return 검색된 장학 공지사항 목록
     */
    List<ScholarshipNotice> findByNoticeDateContainingAndTitleContaining(String date, String title);

    /**
     * 날짜만으로 장학 공지사항을 검색합니다.
     *
     * @param date 날짜 (부분 일치)
     * @return 검색된 장학 공지사항 목록
     */
    List<ScholarshipNotice> findByNoticeDateContaining(String date);

    /**
     * 제목 키워드로 장학 공지사항을 검색합니다.
     *
     * @param title 제목 키워드 (부분 일치)
     * @return 검색된 장학 공지사항 목록
     */
    List<ScholarshipNotice> findByTitleContaining(String title);

    /**
     * 전체 장학 공지사항 목록을 조회합니다.
     *
     * @return 전체 장학 공지사항 목록
     */
    List<ScholarshipNotice> findAll();
}