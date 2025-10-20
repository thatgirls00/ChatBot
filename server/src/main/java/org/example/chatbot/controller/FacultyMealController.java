package org.example.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.FacultyMeal;
import org.example.chatbot.service.FacultyMealService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faculty-meals")
@RequiredArgsConstructor
public class FacultyMealController {

    private final FacultyMealService service;

    @GetMapping("/search")
    public ResponseEntity<List<FacultyMeal>> searchMeals(
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