package org.example.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.service.DormMealFormatterScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final DormMealFormatterScheduler dormMealFormatterScheduler;

    /**
     * 수동 포맷팅 트리거 API
     * POST /api/admin/format-dorm-meal
     */
    @PostMapping("/format-dorm-meal")
    public ResponseEntity<String> triggerDormMealFormatting() {
        dormMealFormatterScheduler.formatDormMeals();
        return ResponseEntity.ok("✅ DormMeal 포맷팅 수동 실행 완료");
    }
}