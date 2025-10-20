package org.example.chatbot.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DateTimeExtractor {

    /**
     * 사용자 입력에서 날짜 범위를 추출합니다. (기본: 오늘~오늘)
     */
    public static LocalDate[] extractDateRange(String userInput) {
        int currentYear = LocalDate.now(Clock.systemUTC()).getYear();
        LocalDate today = LocalDate.now(Clock.systemUTC());

        // 1. 고정 표현 우선 처리
        if (userInput.contains("오늘")) {
            return new LocalDate[]{today, today};
        } else if (userInput.contains("내일")) {
            LocalDate date = today.plusDays(1);
            return new LocalDate[]{date, date};
        } else if (userInput.contains("모레")) {
            LocalDate date = today.plusDays(2);
            return new LocalDate[]{date, date};
        } else if (userInput.contains("어제")) {
            LocalDate date = today.minusDays(1);
            return new LocalDate[]{date, date};
        } else if (userInput.contains("이번주") || userInput.contains("이번 주")) {
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            LocalDate sunday = monday.plusDays(6);
            return new LocalDate[]{monday, sunday};
        } else if (userInput.contains("이번달") || userInput.contains("이번 달")) {
            LocalDate startOfMonth = today.withDayOfMonth(1);
            LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
            return new LocalDate[]{startOfMonth, endOfMonth};
        }

        // 2. MM월 DD일
        Pattern mdPattern = Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})일");
        Matcher mdMatcher = mdPattern.matcher(userInput);
        if (mdMatcher.find()) {
            try {
                int month = Integer.parseInt(mdMatcher.group(1));
                int day = Integer.parseInt(mdMatcher.group(2));
                LocalDate date = LocalDate.of(currentYear, month, day);
                log.debug("[extractDateRange] MM월 DD일 파싱 성공: {}", date);
                return new LocalDate[]{date, date};
            } catch (Exception e) {
                log.debug("[extractDateRange] MM월 DD일 파싱 실패, fallback");
            }
        }

        // 3. MM월 (예: "7월")
        Pattern monthOnlyPattern = Pattern.compile("(\\d{1,2})월");
        Matcher monthMatcher = monthOnlyPattern.matcher(userInput);
        if (monthMatcher.find()) {
            try {
                int month = Integer.parseInt(monthMatcher.group(1));
                LocalDate start = LocalDate.of(currentYear, month, 1);
                LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
                log.debug("[extractDateRange] MM월 파싱 성공: {} ~ {}", start, end);
                return new LocalDate[]{start, end};
            } catch (Exception e) {
                log.debug("[extractDateRange] MM월 파싱 실패, fallback");
            }
        }

        // 4. yyyy-MM-dd 또는 MM-dd
        userInput = userInput.replaceAll("\\.", "-");
        Pattern ymdPattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})|(\\d{2}-\\d{2})");
        Matcher ymdMatcher = ymdPattern.matcher(userInput);
        if (ymdMatcher.find()) {
            String match = ymdMatcher.group();
            try {
                if (match.matches("\\d{2}-\\d{2}")) {
                    match = currentYear + "-" + match;
                }
                LocalDate date = LocalDate.parse(match, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                log.debug("[extractDateRange] yyyy-MM-dd 또는 MM-dd 파싱 성공: {}", date);
                return new LocalDate[]{date, date};
            } catch (Exception e) {
                log.debug("[extractDateRange] yyyy-MM-dd 또는 MM-dd 파싱 실패");
            }
        }

        // 5. fallback
        return new LocalDate[]{today, today};
    }

    /**
     * 사용자 입력에서 식사 시간대 추출.
     */
    public static String extractMealTime(String userInput) {
        if (userInput.contains("맛난")) return "맛난한끼";
        if (userInput.contains("건강")) return "건강한끼";
        if (userInput.contains("아침")) return "아침";
        if (userInput.contains("점심")) return "점심";
        if (userInput.contains("저녁")) return "저녁";
        return null;
    }

    /**
     * 학사일정 content에서 MM.DD (요일) ~ MM.DD (요일) 형태의 기간을 추출합니다.
     */
    public static LocalDate[] extractScheduleDateRange(String content, int year) {
        Pattern pattern = Pattern.compile("(\\d{2}\\.\\d{2})\\s*\\([^)]+\\)\\s*~\\s*(\\d{2}[\\.\\-]\\d{2})");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            try {
                String startStr = matcher.group(1).replace(".", "-");
                String endStr = matcher.group(2).replace(".", "-");

                LocalDate start = LocalDate.parse(year + "-" + startStr);
                LocalDate end = LocalDate.parse(year + "-" + endStr);

                if (end.isBefore(start)) {
                    end = end.plusYears(1);
                }
                return new LocalDate[]{start, end};
            } catch (DateTimeParseException e) {
                return null;
            }
        }

        pattern = Pattern.compile("(\\d{2}\\.\\d{2})\\s*\\([^)]+\\)");
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            try {
                LocalDate date = LocalDate.parse(year + "-" + matcher.group(1).replace(".", "-"));
                return new LocalDate[]{date, date};
            } catch (DateTimeParseException e) {
                return null;
            }
        }

        return null;
    }
}