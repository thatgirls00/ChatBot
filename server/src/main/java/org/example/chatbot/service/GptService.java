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
            "학사공지", "장학공지", "한경공지", "학사일정",
            "식당 미지정", "공지", "공지사항", "전체공지"
    );

    private String forceIntentIfContains(String text) {
        if (text == null) return null;

        Map<String, String> forcedIntentMap = Map.of(
                "학생식당", "학생식당",
                "교직원식당", "교직원식당",
                "기숙사식당", "기숙사식당",
                "학사공지", "학사공지",
                "장학공지", "장학공지",
                "한경공지", "한경공지",
                "학사일정", "학사일정"
        );

        String lower = text.toLowerCase();

        for (Map.Entry<String, String> entry : forcedIntentMap.entrySet()) {
            if (text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        if (lower.contains("한경") && lower.contains("공지")) return "한경공지";
        if (lower.contains("학사") && lower.contains("공지")) return "학사공지";
        if (lower.contains("장학") && lower.contains("공지")) return "장학공지";
        if (lower.contains("전체") && lower.contains("공지")) return "전체공지";
        if (lower.contains("일정")) return "학사일정";
        if (lower.contains("식당")) return "식당 미지정";

        if (lower.contains("공지") || lower.contains("공지사항")) {
            return "공지";
        }

        return null;
    }

    public IntentResultDto classifyIntent(String userInput) {
        String normalizePrompt = GptPromptBuilder.buildNormalizePrompt(userInput);
        String normalized = sendToGpt(normalizePrompt).trim();

        log.info("📥 정제된 문장: {}", normalized);

        String classifyPrompt = GptPromptBuilder.buildClassifyPrompt(normalized);
        String rawContent = sendToGpt(classifyPrompt);
        String content = sanitizeGptResponse(rawContent).trim();

        log.error("📥 GPT 원문 응답(raw): {}", rawContent);
        log.error("📥 GPT 정리된 응답(sanitized): {}", content);

        try {
            if (!content.startsWith("{")) {
                return handleIntentFallback(userInput, content);
            }

            JsonNode root = objectMapper.readTree(content);
            String intent = root.has("intent") ? root.get("intent").asText(null) : null;
            if (intent == null || intent.equals("없음") || !VALID_INTENTS.contains(intent)) {
                intent = forceIntentIfContains(normalized);
                if (intent == null) {
                    intent = forceIntentIfContains(userInput);
                }
            }
            String keyword = root.has("keyword") ? root.get("keyword").asText(null) : null;

            if (userInput.contains("일정")) {
                log.error("📥 질문에 '일정' 키워드 감지, intent를 '학사일정'으로 강제 지정합니다.");
                intent = "학사일정";
            }

            if ("없음".equals(intent) || intent == null) {
                intent = forceIntentIfContains(normalized);
                if (intent == null) {
                    intent = forceIntentIfContains(userInput);
                }
                if (intent != null) {
                    log.warn("📥 GPT 응답이 '없음'이지만 강제로 intent='{}' 지정", intent);
                }
            }

            if (intent == null || !VALID_INTENTS.contains(intent)) {
                return new IntentResultDto("없음", null, "죄송해요, 이해하지 못했어요. 더 구체적으로 말씀해 주세요!");
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
        String rawAnswer = sendToGpt(prompt);
        return stripMarkdown(rawAnswer);
    }

    private String stripMarkdown(String input) {
        if (input == null) return null;
        return input.replace("**", "");
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
        String[] sections = formattedMenu.split("(?=\\[.*?\\])");

        if (sections.length == 3) {
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
                return formattedMenu;
            } else {
                StringBuilder result = new StringBuilder();
                String[] labels = {"[점심]", "[저녁]"};

                for (int i = 0; i < sections.length; i++) {
                    String body = sections[i].replaceFirst("^\\[.*?\\]\\s*", "");
                    result.append(labels[i]).append("\n").append(body.trim()).append("\n\n");
                }

                return result.toString().trim();
            }
        } else if (sections.length == 1) {
            String body = sections[0].replaceFirst("^\\[.*?\\]\\s*", "");
            return "[점심]\n" + body.trim();
        } else {
            return formattedMenu;
        }
    }
}