package org.example.chatbot.dto;

import lombok.Data;

@Data
public class GptRequestDto {
    private String message;
    private String userId; // Redis 키 식별용
}
