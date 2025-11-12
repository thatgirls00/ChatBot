package org.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.util.DateTimeExtractor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SlotResolveService {

    private final ChatSessionService chatSessionService;

    public SlotResult resolve(String userId, String userInput, String intent) {
        boolean isCafeteriaIntent = intent != null && (
                intent.equals("학생식당") || intent.equals("교직원식당") || intent.equals("기숙사식당")
        );

        if (!isCafeteriaIntent) {
            return new SlotResult(null, null);
        }

        LocalDate[] parsedDateRange = DateTimeExtractor.extractDateRange(userInput);
        String parsedDate = parsedDateRange != null ? parsedDateRange[0].toString() : null;
        String parsedCafeteria = extractCafeteria(userInput);

        String lastDate = chatSessionService.getLastDate(userId);
        String lastKeyword = chatSessionService.getLastKeyword(userId);

        String resolvedDate = parsedDate != null ? parsedDate : lastDate;
        String resolvedCafeteria = parsedCafeteria != null ? parsedCafeteria : lastKeyword;

        chatSessionService.saveSession(userId, resolvedDate, resolvedCafeteria, null);

        return new SlotResult(resolvedDate, resolvedCafeteria);
    }

    private String extractCafeteria(String input) {
        if (input.contains("학식") || input.contains("학생식당")) return "학생식당";
        if (input.contains("교식") || input.contains("교직원식당")) return "교직원식당";
        if (input.contains("기식") || input.contains("기숙사식당")) return "기숙사식당";
        return null;
    }

    public record SlotResult(String date, String cafeteria) {
        public boolean isComplete() {
            return date != null && cafeteria != null;
        }
    }
}