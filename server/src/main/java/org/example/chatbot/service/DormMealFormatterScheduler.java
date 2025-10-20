package org.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatbot.domain.DormMeal;
import org.example.chatbot.repository.DormMealRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DormMealFormatterScheduler {

    private final DormMealRepository dormMealRepository;
    private final GptService gptService;

    /**
     * 매일 새벽 3시에 dorm_meals의 메뉴 원본을 GPT로 포맷팅해 formatted_menu에 저장합니다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void formatDormMeals() {
        log.info("✅ DormMeal 포맷팅 스케줄러 실행 시작");

        // formatted_menu가 NULL이거나 menu와 내용이 불일치한 데이터만 가져오기
        List<DormMeal> mealsToFormat = dormMealRepository.findDormMealsToFormat();
        log.info("📌 포맷팅 대상 식단 수: {}", mealsToFormat.size());

        for (DormMeal meal : mealsToFormat) {
            try {
                String formatted = gptService.formatMealWithGpt(meal.getMenu());
                meal.setFormattedMenu(formatted);
                dormMealRepository.save(meal); // JPA dirty checking으로 update
                log.info("✅ [{}] 포맷팅 완료", meal.getMealDate());
            } catch (Exception e) {
                log.error("❗ [{}] 포맷팅 실패: {}", meal.getMealDate(), e.getMessage());
            }
        }

        log.info("✅ DormMeal 포맷팅 스케줄러 실행 종료");
    }
}