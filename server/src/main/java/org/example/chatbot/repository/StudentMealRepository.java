package org.example.chatbot.repository;

import org.example.chatbot.domain.StudentMeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentMealRepository extends JpaRepository<StudentMeal, Long> {

    /**
     * 날짜와 메뉴 키워드로 학생 식단을 검색합니다.
     *
     * @param mealDate 날짜 (부분 일치)
     * @param keyword  메뉴 키워드 (부분 일치)
     * @return 검색된 학생 식단 목록
     */
    List<StudentMeal> findByMealDateContainingAndMenuContaining(String mealDate, String keyword);

    /**
     * 날짜만으로 학생 식단을 검색합니다.
     *
     * @param mealDate 날짜 (부분 일치)
     * @return 검색된 학생 식단 목록
     */
    List<StudentMeal> findByMealDateContaining(String mealDate);

    /**
     * 메뉴 키워드로 학생 식단을 검색합니다.
     *
     * @param keyword 메뉴 키워드 (부분 일치)
     * @return 검색된 학생 식단 목록
     */
    List<StudentMeal> findByMenuContaining(String keyword);

    /**
     * 전체 학생 식단 목록을 조회합니다.
     *
     * @return 전체 학생 식단 목록
     */
    List<StudentMeal> findAll();
}