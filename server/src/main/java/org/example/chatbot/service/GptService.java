package org.example.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatbot.dto.IntentResultDto;
import org.example.chatbot.util.GptPromptBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GptService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.model}")
    private String model;

    private static final Set<String> VALID_INTENTS = Set.of(
            "학생식당", "교직원식당", "기숙사식당",
            "학사공지", "장학공지", "학사일정", "한경공지",
            "식당 미지정"
    );

    public IntentResultDto classifyIntent(String userInput) {
        String prompt = GptPromptBuilder.buildIntentAndKeywordPrompt(userInput);
        String rawContent = sendToGpt(prompt);
        String content = sanitizeGptResponse(rawContent).trim();

        log.error("📥 GPT 원문 응답(raw): {}", rawContent);
        log.error("📥 GPT 정리된 응답(sanitized): {}", content);

        try {
            if (!content.startsWith("{")) {
                return handleIntentFallback(userInput, content);
            }

            JsonNode root = objectMapper.readTree(content);
            String intent = root.has("intent") ? root.get("intent").asText(null) : null;
            String keyword = root.has("keyword") ? root.get("keyword").asText(null) : null;

            if (userInput.contains("일정")) {
                log.error("📥 질문에 '일정' 키워드 감지, intent를 '학사일정'으로 강제 지정합니다.");
                intent = "학사일정";
            }

            if (intent == null || !VALID_INTENTS.contains(intent)) {
                if (userInput.contains("식당")) {
                    return new IntentResultDto("식당 미지정", null,
                            "어느 식당의 식단이 궁금하신가요? 학생식당, 교직원식당, 기숙사식당 중 선택해 주세요.");
                }
                return new IntentResultDto("없음", null, null);
            }

            return new IntentResultDto(intent, keyword, null);

        } catch (Exception e) {
            return handleIntentFallback(userInput, content);
        }
    }

    private IntentResultDto handleIntentFallback(String userInput, String content) {
        if (userInput.contains("식당")) {
            log.error("📥 fallback에서도 식당 키워드로 intent를 '식당 미지정'으로 보정합니다.");
            return new IntentResultDto("식당 미지정", null,
                    "어느 식당의 식단이 궁금하신가요? 학생식당, 교직원식당, 기숙사식당 중 선택해 주세요.");
        }
        return new IntentResultDto("없음", null, content);
    }

    public String generateFallbackAnswer(String userInput) {
        String prompt = GptPromptBuilder.buildFallbackPrompt(userInput);
        return sendToGpt(prompt);
    }

    public String formatMealWithGpt(String rawMenu) {
        String prompt = String.format(
                """
                아래 기숙사 식단 메뉴를 시간대별로 [아침], [점심], [저녁] 태그를 붙여 구분하고, 각 항목은 - 기호로 줄바꿈해 깔끔하게 출력해줘.
                다른 텍스트는 절대 추가하지 말고, 메뉴 내용만 다음 예시와 같은 형태로 반환해:

                [점심] 12:00~13:00
                - 귀리밥
                - 소고기무국 (호주산)
                ...
                
                [저녁] 17:00~18:10
                - 참치김치밥
                ...
                
                만약 [아침], [점심], [저녁] 시간대가 명확하지 않다면 절대로 [전체] 같은 임의의 태그를 넣지 말고, 그냥 항목만 - 기호로 나열해줘.
                
                아래는 메뉴 원본이다:
                %s
                """,
                rawMenu
        );
        String gptResult = sendToGpt(prompt).trim();
        return postProcessFormattedMenu(gptResult);
    }

    private String sendToGpt(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.0,
                "max_tokens", 500,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            return extractContentFromResponse(response);
        } catch (Exception e) {
            log.error("❗ GPT 호출 실패: {}", e.getMessage());
            return "메뉴 포맷팅에 실패했습니다.";
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContentFromResponse(ResponseEntity<Map> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return message.get("content").toString().trim();
    }

    private String sanitizeGptResponse(String content) {
        return content.replaceAll("[\\u0000-\\u001F\\u007F\\uFEFF-\\uFFFF]", "").trim();
    }

    public String postProcessFormattedMenu(String formattedMenu) {
        // 섹션별 나누기 (예: [아침], [점심] 등으로 나눔)
        String[] sections = formattedMenu.split("(?=\\[.*?\\])"); // "[점심]" 같은 태그 앞에서 split

        if (sections.length == 3) {
            // 아침/점심/저녁 3개 → 그대로 반환
            return formattedMenu;
        } else if (sections.length == 2) {
            boolean hasTime = false;

            for (String section : sections) {
                if (section.contains("12:") || section.contains("13:") || section.contains("17:") || section.contains("18:")) {
                    hasTime = true;
                    break;
                }
            }

            if (hasTime) {
                // 시간대가 있으면 그대로 유지
                return formattedMenu;
            } else {
                // 시간대 없으면 [점심], [저녁]으로 교체
                StringBuilder result = new StringBuilder();
                String[] labels = {"[점심]", "[저녁]"};

                for (int i = 0; i < sections.length; i++) {
                    // 기존 헤더 제거 후 새로운 헤더 붙이기
                    String body = sections[i].replaceFirst("^\\[.*?\\]\\s*", ""); // 기존 [헤더] 제거
                    result.append(labels[i]).append("\n").append(body.trim()).append("\n\n");
                }

                return result.toString().trim();
            }
        } else if (sections.length == 1) {
            // 기존 [전체] 또는 [식사] 태그를 제거하고 [점심]으로 고정해 붙이기
            String body = sections[0].replaceFirst("^\\[.*?\\]\\s*", "");
            return "[점심]\n" + body.trim();
        } else {
            // 그 외 이상한 경우에도 그대로 반환 (예외 방지용)
            return formattedMenu;
        }
    }
}