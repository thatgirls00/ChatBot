package org.example.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.AcademicNotice;
import org.example.chatbot.service.AcademicNoticeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-notices")
@RequiredArgsConstructor
public class AcademicNoticeController {

    private final AcademicNoticeService service;

    @GetMapping("/search")
    public ResponseEntity<List<AcademicNotice>> searchNotices(
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