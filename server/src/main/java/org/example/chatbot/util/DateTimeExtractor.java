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

    public static LocalDate[] extractDateRange(String userInput) {
        int currentYear = LocalDate.now(Clock.systemUTC()).getYear();
        LocalDate today = LocalDate.now(Clock.systemUTC());

        if (userInput.contains("오늘")) return new LocalDate[]{today, today};
        if (userInput.contains("내일")) return new LocalDate[]{today.plusDays(1), today.plusDays(1)};
        if (userInput.contains("모레")) return new LocalDate[]{today.plusDays(2), today.plusDays(2)};
        if (userInput.contains("어제")) return new LocalDate[]{today.minusDays(1), today.minusDays(1)};
        if (userInput.contains("그제") || userInput.contains("그저께")) return new LocalDate[]{today.minusDays(2), today.minusDays(2)};
        if (userInput.contains("이번주") || userInput.contains("이번 주")) {
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            return new LocalDate[]{monday, monday.plusDays(6)};
        }
        if (userInput.contains("다음주") || userInput.contains("다음 주")) {
            LocalDate nextMonday = today.with(DayOfWeek.MONDAY).plusWeeks(1);
            return new LocalDate[]{nextMonday, nextMonday.plusDays(6)};
        }
        if (userInput.contains("지난주") || userInput.contains("지난 주") || userInput.contains("저번주") || userInput.contains("저번 주")) {
            LocalDate lastMonday = today.with(DayOfWeek.MONDAY).minusWeeks(1);
            return new LocalDate[]{lastMonday, lastMonday.plusDays(6)};
        }
        if (userInput.contains("이번달") || userInput.contains("이번 달")) {
            LocalDate start = today.withDayOfMonth(1);
            return new LocalDate[]{start, start.withDayOfMonth(start.lengthOfMonth())};
        }
        if (userInput.contains("지난달") || userInput.contains("지난 달") || userInput.contains("저번달") || userInput.contains("저번 달")) {
            LocalDate start = today.minusMonths(1).withDayOfMonth(1);
            return new LocalDate[]{start, start.withDayOfMonth(start.lengthOfMonth())};
        }
        if (userInput.contains("다음달") || userInput.contains("다음 달")) {
            LocalDate start = today.plusMonths(1).withDayOfMonth(1);
            return new LocalDate[]{start, start.withDayOfMonth(start.lengthOfMonth())};
        }

        Matcher mdMatcher = Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})일").matcher(userInput);
        if (mdMatcher.find()) {
            try {
                int month = Integer.parseInt(mdMatcher.group(1));
                int day = Integer.parseInt(mdMatcher.group(2));
                LocalDate date = LocalDate.of(currentYear, month, day);
                log.debug("[extractDateRange] MM월 DD일 파싱 성공: {}", date);
                return new LocalDate[]{date, date};
            } catch (Exception e) {
                log.warn("[extractDateRange] MM월 DD일 파싱 실패: {}", e.getMessage());
            }
        }

        Matcher monthMatcher = Pattern.compile("(\\d{1,2})월").matcher(userInput);
        if (monthMatcher.find()) {
            try {
                int month = Integer.parseInt(monthMatcher.group(1));
                LocalDate start = LocalDate.of(currentYear, month, 1);
                return new LocalDate[]{start, start.withDayOfMonth(start.lengthOfMonth())};
            } catch (Exception e) {
                log.warn("[extractDateRange] MM월 파싱 실패: {}", e.getMessage());
            }
        }

        Matcher fullDateMatcher = Pattern.compile("(\\d{4})[\\.\\-](\\d{1,2})[\\.\\-](\\d{1,2})").matcher(userInput);
        if (fullDateMatcher.find()) {
            try {
                int year = Integer.parseInt(fullDateMatcher.group(1));
                int month = Integer.parseInt(fullDateMatcher.group(2));
                int day = Integer.parseInt(fullDateMatcher.group(3));
                LocalDate date = LocalDate.of(year, month, day);
                log.debug("[extractDateRange] yyyy-MM-dd 파싱 성공: {}", date);
                return new LocalDate[]{date, date};
            } catch (Exception e) {
                log.warn("[extractDateRange] yyyy-MM-dd 파싱 실패: {}", e.getMessage());
            }
        }

        Matcher shortDateMatcher = Pattern.compile("(\\d{1,2})[\\.\\-](\\d{1,2})").matcher(userInput);
        if (shortDateMatcher.find()) {
            try {
                int month = Integer.parseInt(shortDateMatcher.group(1));
                int day = Integer.parseInt(shortDateMatcher.group(2));
                LocalDate date = LocalDate.of(currentYear, month, day);
                log.debug("[extractDateRange] MM-dd 파싱 성공: {}", date);
                return new LocalDate[]{date, date};
            } catch (Exception e) {
                log.warn("[extractDateRange] MM-dd 파싱 실패: {}", e.getMessage());
            }
        }

        return new LocalDate[]{today, today};
    }

    public static String extractMealTime(String userInput) {
        if (userInput.contains("맛난")) return "맛난한끼";
        if (userInput.contains("건강")) return "건강한끼";
        if (userInput.contains("아침")) return "아침";
        if (userInput.contains("점심")) return "점심";
        if (userInput.contains("저녁")) return "저녁";
        return null;
    }

    public static LocalDate[] extractScheduleDateRange(String content, int year) {
        Pattern pattern = Pattern.compile("(\\d{2}\\.\\d{2})\\s*\\([^)]+\\)\\s*~\\s*(\\d{2}[\\.\\-]\\d{2})");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            try {
                String startStr = matcher.group(1).replace(".", "-");
                String endStr = matcher.group(2).replace(".", "-");
                LocalDate start = LocalDate.parse(year + "-" + startStr);
                LocalDate end = LocalDate.parse(year + "-" + endStr);
                if (end.isBefore(start)) end = end.plusYears(1);
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

    public static boolean containsDateKeyword(String input) {
        if (input == null || input.isBlank()) return false;
        return input.matches(".*(오늘|내일|모레|어제|이번주|이번 주|다음주|다음 주|지난주|저번주|이번달|이번 달|다음달|다음 달|지난달|저번달).*");
    }
}