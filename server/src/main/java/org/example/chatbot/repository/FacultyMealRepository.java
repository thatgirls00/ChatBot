package org.example.chatbot.repository;

import org.example.chatbot.domain.FacultyMeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacultyMealRepository extends JpaRepository<FacultyMeal, Long> {

    /**
     * 날짜와 메뉴를 기준으로 교직원 식단을 검색합니다.
     *
     * @param mealDate 날짜 (부분 일치)
     * @param keyword 메뉴 키워드 (부분 일치)
     * @return 검색된 식단 목록
     */
    List<FacultyMeal> findByMealDateContainingAndMenuContaining(String mealDate, String keyword);

    /**
     * 날짜만으로 교직원 식단을 검색합니다.
     *
     * @param mealDate 날짜 (부분 일치)
     * @return 검색된 식단 목록
     */
    List<FacultyMeal> findByMealDateContaining(String mealDate);

    /**
     * 메뉴 키워드로 교직원 식단을 검색합니다.
     *
     * @param keyword 메뉴 키워드 (부분 일치)
     * @return 검색된 식단 목록
     */
    List<FacultyMeal> findByMenuContaining(String keyword);

    /**
     * 전체 교직원 식단 목록을 조회합니다.
     *
     * @return 전체 식단 목록
     */
    List<FacultyMeal> findAll();
}