package org.example.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.dto.*;
import org.example.chatbot.service.*;
import org.example.chatbot.util.DateTimeExtractor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.example.chatbot.util.DateTimeExtractor.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class GptController {

    private final GptService gptService;
    private final NoticeService noticeService;
    private final TableQueryService tableQueryService;
    private final ChatSessionService chatSessionService;

    private static final Set<String> MEAL_INTENTS = Set.of("학생식당", "교직원식당", "기숙사식당");
    private static final Set<String> NOTICE_INTENTS = Set.of("학사공지", "장학공지", "한경공지");
    private static final String SCHEDULE_INTENT = "학사일정";
    private static final String NOTICE_ALL_INTENT = "전체공지";

    @GetMapping("/intent")
    public ResponseEntity<GptResponseDto> getSession(@RequestParam("userId") String userId) {
        return ResponseEntity.ok(new GptResponseDto(
                null,
                "안녕하세요! 한경국립대학교 챗봇입니다. \n학사공지, 학사일정, 식단 등을 편하게 물어보세요. 예: '7월 학사일정 알려줘', '오늘 기숙사식당 메뉴 알려줘' 등"
        ));
    }

    @GetMapping("/notices")
    public ResponseEntity<List<NoticeDto>> getNoticesByGptIntent(
            @RequestParam String intent,
            @RequestParam(required = false) String keyword
    ) {
        LocalDate today = LocalDate.now();
        return ResponseEntity.ok(
                noticeService.searchNotices(intent, keyword, today, today)
        );
    }

    @PostMapping("/intent")
    public ResponseEntity<GptResponseDto> handleUserInput(@RequestBody GptRequestDto request) {
        String userInput = request.getMessage();
        String lowerInput = userInput.toLowerCase();

        IntentResultDto result = gptService.classifyIntent(userInput);
        String intent = Optional.ofNullable(result.getIntent()).orElse("").trim();
        String keyword = result.getKeyword();
        String answer = result.getAnswer();

        if (intent.isBlank() || "없음".equalsIgnoreCase(intent)) {
            if (lowerInput.contains("공지") || lowerInput.contains("공지사항")) {
                intent = NOTICE_ALL_INTENT;
            } else {
                return ResponseEntity.ok(
                        new GptResponseDto("없음", gptService.generateFallbackAnswer(userInput))
                );
            }
        }

        if (DateTimeExtractor.containsDateKeyword(keyword)) {
            keyword = null;
        }

        LocalDate[] dateRange = extractDateRange(userInput);
        LocalDate startDate = Optional.ofNullable(dateRange[0]).orElse(LocalDate.now());
        LocalDate endDate = Optional.ofNullable(dateRange[1]).orElse(LocalDate.now());

        boolean dateFilterApplied =
                !(startDate.equals(endDate) && startDate.equals(LocalDate.now())) ||
                        containsDateKeyword(userInput);

        String mealTime = adjustMealTime(intent, userInput, extractMealTime(userInput));

        if ("식당 미지정".equalsIgnoreCase(intent)) {
            return ResponseEntity.ok(new GptResponseDto(
                    "식당 미지정",
                    "어느 식당의 식단이 궁금하신가요? 학생식당, 교직원식당, 기숙사식당 중 선택해 주세요."
            ));
        }

        if (NOTICE_ALL_INTENT.equals(intent)) {
            StringBuilder answerBuilder = new StringBuilder();

            // 학사공지
            List<?> academicList = tableQueryService.findNoticeDataByIntent("학사공지", keyword);
            String academicAnswer = tableQueryService.filterNoticeByConditions(keyword, startDate, endDate, dateFilterApplied, academicList);
            if (!academicAnswer.contains("찾을 수 없습니다")) {
                answerBuilder.append("📚 [학사공지]\n").append(academicAnswer).append("\n\n");
            }

            // 장학공지
            List<?> scholarshipList = tableQueryService.findNoticeDataByIntent("장학공지", keyword);
            String scholarshipAnswer = tableQueryService.filterNoticeByConditions(keyword, startDate, endDate, dateFilterApplied, scholarshipList);
            if (!scholarshipAnswer.contains("찾을 수 없습니다")) {
                answerBuilder.append("🎓 [장학공지]\n").append(scholarshipAnswer).append("\n\n");
            }

            // 한경공지
            List<?> hankyongList = tableQueryService.findNoticeDataByIntent("한경공지", keyword);
            String hankyongAnswer = tableQueryService.filterNoticeByConditions(keyword, startDate, endDate, dateFilterApplied, hankyongList);
            if (!hankyongAnswer.contains("찾을 수 없습니다")) {
                answerBuilder.append("🏫 [한경공지]\n").append(hankyongAnswer).append("\n\n");
            }

            String finalAnswer = answerBuilder.toString().trim();
            if (finalAnswer.isBlank()) {
                finalAnswer = "요청하신 기간에 등록된 공지사항이 없습니다.";
            }

            chatSessionService.saveSession(request.getUserId(), intent, startDate.toString(), keyword, null);
            return ResponseEntity.ok(new GptResponseDto(intent, finalAnswer));
        }

        if (MEAL_INTENTS.contains(intent)) {
            if (!dateFilterApplied) {
                return ResponseEntity.ok(new GptResponseDto(
                        intent, "어느 날짜의 메뉴가 궁금하신가요? 예: 오늘, 내일, 7월 8일 등으로 입력해 주세요."
                ));
            }
            List<?> dataList = tableQueryService.findMealDataByIntent(intent, keyword);
            String mealAnswer = tableQueryService.filterMealByConditions(intent, keyword, mealTime, startDate, endDate, dateFilterApplied, dataList);
            chatSessionService.saveSession(request.getUserId(), intent, startDate.toString(), keyword, mealTime);
            return ResponseEntity.ok(new GptResponseDto(intent, mealAnswer));
        }

        if (NOTICE_INTENTS.contains(intent)) {
            if ((keyword == null || keyword.isBlank()) && !dateFilterApplied) {
                return ResponseEntity.ok(new GptResponseDto(intent, buildReaskMessage(intent)));
            }
            List<?> dataList = tableQueryService.findNoticeDataByIntent(intent, keyword);
            String noticeAnswer = tableQueryService.filterNoticeByConditions(keyword, startDate, endDate, dateFilterApplied, dataList);
            chatSessionService.saveSession(request.getUserId(), intent, startDate.toString(), keyword, mealTime);
            return ResponseEntity.ok(new GptResponseDto(intent, noticeAnswer));
        }

        if (SCHEDULE_INTENT.equals(intent)) {
            keyword = normalizeKeyword(keyword);
            List<?> dataList = tableQueryService.findNoticeDataByIntent(intent, null);
            String scheduleAnswer = tableQueryService.filterAcademicScheduleByConditions(keyword, startDate, endDate, dateFilterApplied, dataList);

            if (scheduleAnswer.isBlank() && keyword != null && !keyword.isBlank()) {
                String otherDate = tableQueryService.findKeywordInOtherDates(keyword, startDate, endDate);
                scheduleAnswer = !otherDate.isBlank()
                        ? String.format("요청하신 기간에는 '%s' 일정이 없지만, %s에 같은 일정이 있습니다.", keyword, otherDate)
                        : String.format("'%s' 키워드에 해당하는 학사일정을 찾을 수 없습니다.", keyword);
            }

            if (scheduleAnswer.isBlank()) {
                scheduleAnswer = "어떤 학사일정을 찾으시나요? 예: 수강신청, 휴학 등 키워드를 입력해 주세요.";
            }

            chatSessionService.saveSession(request.getUserId(), intent, startDate.toString(), keyword, mealTime);
            return ResponseEntity.ok(new GptResponseDto(intent, scheduleAnswer));
        }

        return ResponseEntity.ok(new GptResponseDto("없음", gptService.generateFallbackAnswer(userInput)));
    }

    private String adjustMealTime(String intent, String input, String mealTime) {
        if ("학생식당".equals(intent)) {
            if ("점심".equals(mealTime)) return null;
            if (mealTime == null || mealTime.isBlank()) {
                if (input.contains("건강한끼")) return "건강한끼";
                if (input.contains("맛난한끼")) return "맛난한끼";
            }
        }
        return mealTime;
    }

    private boolean containsDateKeyword(String input) {
        return input.contains("오늘") || input.contains("어제") || input.contains("내일") ||
                input.contains("모레") || input.contains("이번주") || input.contains("이번 주") ||
                input.contains("이번달") || input.contains("이번 달") || input.contains("지난주") ||
                input.contains("저번주") || input.contains("지난달") || input.contains("저번달") ||
                input.contains("다음주") || input.contains("다음달");
    }

    private String buildReaskMessage(String intent) {
        return switch (intent) {
            case "학사공지" -> "학사공지에서 언제의 어떤 내용을 찾으시나요? 예: 이번 달 학사공지 뭐야?";
            case "장학공지" -> "장학공지에서 언제의 어떤 정보를 찾으시나요? 예: 이번 달 장학공지 뭐야?";
            case "한경공지" -> "한경공지에서 언제의 어떤 내용을 찾으시나요? 예: 이번 달 한경공지 뭐야?";
            case "학사일정" -> "어느 시기의 학사일정을 찾으시나요? 예: 2학기 수강신청, 겨울방학 시작일 등.";
            default -> "찾고자 하는 정보의 날짜나 키워드를 입력해 주세요.";
        };
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) return null;
        if (keyword.contains("졸업")) return "학위수여";
        if (keyword.contains("학위수여")) return "졸업";
        return keyword;
    }
}