package org.example.chatbot.repository;

import org.example.chatbot.domain.DormMeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DormMealRepository extends JpaRepository<DormMeal, Long> {

    /**
     * 날짜와 메뉴를 기준으로 기숙사 식단을 검색합니다.
     *
     * @param mealDate 날짜 (부분 일치)
     * @param keyword 메뉴 키워드 (부분 일치)
     * @return 검색된 식단 목록
     */
    List<DormMeal> findByMealDateContainingAndMenuContaining(String mealDate, String keyword);

    /**
     * 날짜만으로 기숙사 식단을 검색합니다.
     *
     * @param mealDate 날짜 (부분 일치)
     * @return 검색된 식단 목록
     */
    List<DormMeal> findByMealDateContaining(String mealDate);

    /**
     * 메뉴 키워드로 기숙사 식단을 검색합니다.
     *
     * @param keyword 메뉴 키워드 (부분 일치)
     * @return 검색된 식단 목록
     */
    List<DormMeal> findByMenuContaining(String keyword);

    /**
     * 전체 기숙사 식단 목록을 조회합니다.
     *
     * @return 전체 식단 목록
     */
    List<DormMeal> findAll();

    /**
     * formatted_menu가 비어있거나 NULL인 기숙사 식단을 조회합니다.
     * GPT 포맷팅 대상 데이터를 찾기 위해 사용합니다.
     *
     * @return 포맷팅 대상 식단 목록
     */
    @Query("SELECT d FROM DormMeal d WHERE d.formattedMenu IS NULL OR d.formattedMenu = ''")
    List<DormMeal> findDormMealsToFormat();
}