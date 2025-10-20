package org.example.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.StudentMeal;
import org.example.chatbot.service.StudentMealService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student-meals")
@RequiredArgsConstructor
public class StudentMealController {

    private final StudentMealService service;

    @GetMapping("/search")
    public ResponseEntity<List<StudentMeal>> searchMeals(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String keyword) {

        return ResponseEntity.ok(service.search(date, keyword));
    }

    @PostMapping("/clear-cache")
    public ResponseEntity<Void> clearCache() {
        service.clearCache();
        return ResponseEntity.ok().build();
    }
}