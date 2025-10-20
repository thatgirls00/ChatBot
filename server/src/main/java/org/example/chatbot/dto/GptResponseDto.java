package org.example.chatbot.dto;

import lombok.Data;

@Data
public class GptResponseDto {

    private String intent;
    private String answer;      // fallback용
    private Object data;        // 테이블 결과용

    public GptResponseDto(String intent, String answer) {
        this.intent = intent;
        this.answer = answer;
    }

    public GptResponseDto(String intent, Object data) {
        this.intent = intent;
        this.data = data;
    }
}