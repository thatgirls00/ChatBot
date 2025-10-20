package org.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatbot.domain.*;
import org.example.chatbot.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.example.chatbot.util.DateTimeExtractor.extractScheduleDateRange;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableQueryService {

    private final StudentMealRepository studentMealRepository;
    private final FacultyMealRepository facultyMealRepository;
    private final DormMealRepository dormMealRepository;

    private final AcademicNoticeRepository academicNoticeRepository;
    private final AcademicScheduleRepository academicScheduleRepository;
    private final HankyongNoticeRepository hankyongNoticeRepository;
    private final ScholarshipNoticeRepository scholarshipNoticeRepository;

    public List<?> findMealDataByIntent(String intent, String keyword) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        return switch (intent) {
            case "학생식당" -> studentMealRepository.findAll();
            case "교직원식당" -> hasKeyword
                    ? facultyMealRepository.findByMenuContaining(keyword)
                    : facultyMealRepository.findAll();
            case "기숙사식당" -> dormMealRepository.findAll();
            default -> List.of();
        };
    }

    public List<?> findNoticeDataByIntent(String intent, String keyword) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        return switch (intent) {
            case "학사공지" -> hasKeyword
                    ? academicNoticeRepository.findByTitleContaining(keyword)
                    : academicNoticeRepository.findAll();
            case "장학공지" -> hasKeyword
                    ? scholarshipNoticeRepository.findByTitleContaining(keyword)
                    : scholarshipNoticeRepository.findAll();
            case "한경공지" -> hasKeyword
                    ? hankyongNoticeRepository.findByTitleContaining(keyword)
                    : hankyongNoticeRepository.findAll();
            case "학사일정" -> academicScheduleRepository.findAll();
            default -> List.of();
        };
    }

    public String filterMealByConditions(String intent, String keyword, String mealTime,
                                         LocalDate startDate, LocalDate endDate,
                                         boolean dateFilterApplied, List<?> dataList) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean keywordFound = false;
        boolean foundDateInRange = false;

        Map<String, List<String>> groupedMenus = new LinkedHashMap<>();

        log.info("🔍 filterMealByConditions: mealTime={}, startDate={}, endDate={}", mealTime, startDate, endDate);

        for (Object data : dataList) {
            String menu = null, dateStr = null;

            if (data instanceof DormMeal meal) {
                menu = meal.getFormattedMenu() != null ? meal.getFormattedMenu() : meal.getMenu();
                dateStr = meal.getMealDate();
                if (dateStr == null || dateStr.isBlank() || menu == null || menu.isBlank()) continue;

                LocalDate mealDate = LocalDate.parse(dateStr);
                if (!mealDate.isBefore(startDate) && !mealDate.isAfter(endDate)) {
                    foundDateInRange = true;

                    String extractedMenu = (mealTime != null && !mealTime.isBlank())
                            ? extractMealSection(menu, mealTime)
                            : menu;

                    if (extractedMenu == null || extractedMenu.isBlank()) continue;
                    if (hasKeyword && extractedMenu.contains(keyword)) keywordFound = true;
                    else if (hasKeyword && !extractedMenu.contains(keyword)) continue;

                    // [전체] 생략 조건 분기
                    String formatted = (mealTime != null)
                            ? String.format("[%s]\n%s", mealTime, extractedMenu)
                            : extractedMenu;

                    groupedMenus.computeIfAbsent(dateStr, k -> new ArrayList<>()).add(formatted);
                }
            }

            else if (data instanceof StudentMeal meal) {
                menu = meal.getMenu();
                dateStr = meal.getMealDate();
                String studentMealTime = meal.getMealTime(); // ex: "건강한끼(11:30~13:30)"
                if (menu == null || dateStr == null || studentMealTime == null) continue;

                LocalDate mealDate = LocalDate.parse(dateStr);
                if (!mealDate.isBefore(startDate) && !mealDate.isAfter(endDate)) {
                    foundDateInRange = true;

                    String timeLabel = extractTimeLabel(studentMealTime);  // "건강한끼" or "맛난한끼"
                    String timeRange = extractTimeRange(studentMealTime);  // "11:30~13:30"

                    // "점심"은 전체 포함, 특정 식단명을 지정한 경우만 필터링
                    if (mealTime != null && !mealTime.isBlank() &&
                            !mealTime.equals("점심") &&
                            !normalizeKorean(timeLabel).equalsIgnoreCase(normalizeKorean(mealTime))) continue;

                    // 조건: 학생식당이고 mealTime이 지정된 경우 -> 키워드 필터는 생략
                    boolean skipKeywordCheck = "학생식당".equals(intent) && mealTime != null && !mealTime.equals("점심");

                    if (hasKeyword && !skipKeywordCheck && !menu.contains(keyword)) continue;
                    if (hasKeyword) keywordFound = true;

                    String formatted = String.format("[%s] %s\n%s", timeLabel, timeRange,
                            Arrays.stream(menu.split("\n")).map(s -> "- " + s).collect(Collectors.joining("\n")));

                    groupedMenus.computeIfAbsent(dateStr, k -> new ArrayList<>()).add(formatted);
                }
            }

            else if (data instanceof FacultyMeal meal) {
                menu = meal.getMenu();
                dateStr = meal.getMealDate();
                String facultyMealTime = meal.getMealTime(); // ex: "점심(11:30~13:00)"
                if (menu == null || dateStr == null || facultyMealTime == null) continue;

                LocalDate mealDate = LocalDate.parse(dateStr);
                if (!mealDate.isBefore(startDate) && !mealDate.isAfter(endDate)) {
                    foundDateInRange = true;

                    String timeLabel = extractTimeLabel(facultyMealTime);  // "점심"
                    String timeRange = extractTimeRange(facultyMealTime);  // "11:30~13:00"

                    if (mealTime != null && !mealTime.equals(timeLabel)) continue;
                    if (hasKeyword && !menu.contains(keyword)) continue;
                    if (hasKeyword) keywordFound = true;

                    String formatted = String.format("[%s] %s\n%s", timeLabel, timeRange,
                            Arrays.stream(menu.split("\n")).map(s -> "- " + s).collect(Collectors.joining("\n")));

                    groupedMenus.computeIfAbsent(dateStr, k -> new ArrayList<>()).add(formatted);
                }
            }
        }

        if (!groupedMenus.isEmpty()) {
            List<String> result = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : groupedMenus.entrySet()) {
                StringBuilder sb = new StringBuilder();
                sb.append("[").append(entry.getKey()).append("]\n");

                // 중복 제거 + 빈 메뉴 제외
                List<String> menus = entry.getValue().stream()
                        .distinct()
                        .filter(m -> !m.contains("등록된 식단내용이(가) 없습니다."))
                        .collect(Collectors.toList());

                if (menus.isEmpty()) {
                    sb.append("- 등록된 식단내용이(가) 없습니다.");
                } else {
                    for (String menu : menus) {
                        sb.append(menu).append("\n\n");
                    }
                }
                result.add(sb.toString().trim());
            }

            if (hasKeyword && !keywordFound) {
                return "요청하신 조건에 맞는 식단을 찾지 못했어요.";
            }

            return String.join("\n\n", result);
        }

        if (dateFilterApplied) {
            if (!foundDateInRange) {
                return String.format("요청하신 기간(%s ~ %s)에는 식단 정보가 없어요. 다른 기간으로 다시 질문해 보시겠어요?", startDate, endDate);
            }
            if (hasKeyword) {
                return String.format("요청하신 기간(%s ~ %s)에는 식단은 있지만, '%s' 메뉴는 포함되어 있지 않아요.", startDate, endDate, keyword);
            }
            return String.format("요청하신 기간(%s ~ %s)에는 조건에 맞는 식단을 찾지 못했어요.", startDate, endDate);
        }

        return "최근 관련 식단을 찾지 못했어요.";
    }

    private String extractMealSection(String menu, String mealTime) {
        if (menu == null || mealTime == null) return null;
        String marker = "[" + mealTime + "]";
        int start = menu.indexOf(marker);
        if (start == -1) return null;
        int nextMarker = menu.indexOf("[", start + marker.length());
        if (nextMarker == -1) nextMarker = menu.length();
        return menu.substring(start + marker.length(), nextMarker).trim();
    }

    public String filterNoticeByConditions(String keyword, LocalDate startDate, LocalDate endDate,
                                           boolean dateFilterApplied, List<?> dataList) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        Set<String> matchedNotices = new LinkedHashSet<>();
        Set<String> fallbackNotices = new LinkedHashSet<>();
        boolean foundDateInRange = false;
        int currentYear = LocalDate.now().getYear();

        for (Object data : dataList) {
            String title = null, dateStr = null;

            if (data instanceof AcademicNotice notice) {
                title = notice.getTitle();
                dateStr = notice.getNoticeDate();
            } else if (data instanceof ScholarshipNotice notice) {
                title = notice.getTitle();
                dateStr = notice.getNoticeDate();
            } else if (data instanceof HankyongNotice notice) {
                title = notice.getTitle();
                dateStr = notice.getNoticeDate();
            } else continue;

            if (dateStr == null || dateStr.isBlank()) continue;
            LocalDate noticeDate = LocalDate.parse(dateStr);

            if (!noticeDate.isBefore(startDate) && !noticeDate.isAfter(endDate)) {
                foundDateInRange = true;
                if (!hasKeyword || (title != null && title.contains(keyword))) {
                    matchedNotices.add(String.format("[%s] %s", noticeDate, title));
                }
            } else {
                boolean isThisYear = noticeDate.getYear() == currentYear;
                if (hasKeyword && isThisYear && title != null && title.contains(keyword)) {
                    fallbackNotices.add(String.format("[다른 날짜 %s] %s", dateStr, title));
                }
            }
        }

        if (!matchedNotices.isEmpty()) return String.join("\n\n", matchedNotices);

        if (dateFilterApplied && !foundDateInRange && !fallbackNotices.isEmpty()) {
            return String.format(
                    "요청하신 기간(%s ~ %s)에는 '%s' 키워드를 포함한 공지사항이 없어요.\n다른 날짜에 찾은 관련 공지사항은 다음과 같아요:\n\n%s",
                    startDate, endDate, keyword, String.join("\n\n", fallbackNotices)
            );
        }

        if (dateFilterApplied) {
            return String.format("요청하신 기간(%s ~ %s)에는 '%s' 키워드를 포함한 공지사항이 없어요. 다른 기간으로 다시 질문해 보시겠어요?", startDate, endDate, keyword);
        }

        if (!fallbackNotices.isEmpty()) {
            return "최근 관련 공지사항은 다음과 같아요:\n\n" + String.join("\n\n", fallbackNotices);
        }

        return "최근 관련 공지사항을 찾지 못했어요.";
    }

    public String filterAcademicScheduleByConditions(String keyword, LocalDate startDate, LocalDate endDate,
                                                     boolean dateFilterApplied, List<?> dataList) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        Set<String> matchedSchedules = new LinkedHashSet<>();
        boolean foundDateInRange = false;

        // 연/월만 있을 경우 대비: 기본값 보정
        if (startDate == null && endDate == null && !dateFilterApplied) {
            log.warn("📆 날짜 필터 없음 → 현재 월 전체로 보정합니다.");
            LocalDate now = LocalDate.now();
            startDate = now.withDayOfMonth(1);
            endDate = now.withDayOfMonth(now.lengthOfMonth());
            dateFilterApplied = true;
        }

        int baseYear = (startDate != null) ? startDate.getYear() : LocalDate.now().getYear();

        for (Object data : dataList) {
            if (!(data instanceof AcademicSchedule schedule)) continue;
            String content = schedule.getContent();
            if (content == null || content.isBlank()) continue;

            // 키워드 필터 우선 적용
            if (hasKeyword && !content.contains(keyword)) continue;

            LocalDate[] scheduleRange = extractScheduleDateRange(content, baseYear);
            if (scheduleRange == null) continue;

            LocalDate scheduleStart = scheduleRange[0], scheduleEnd = scheduleRange[1];
            boolean isOverlap = (startDate == null || !scheduleStart.isAfter(endDate)) &&
                    (endDate == null || !scheduleEnd.isBefore(startDate));

            if (!dateFilterApplied || isOverlap) {
                if (isOverlap) foundDateInRange = true;
                matchedSchedules.add(String.format("[%s ~ %s] %s", scheduleStart, scheduleEnd, content));
            }
        }

        if (!matchedSchedules.isEmpty()) {
            return String.join("\n\n", matchedSchedules);
        }

        // 키워드만 있고 날짜 필터가 없을 경우 별도 처리 (중복 방지 위해 별도 Set 사용)
        if (!dateFilterApplied && hasKeyword) {
            Set<String> keywordOnlyMatches = new LinkedHashSet<>();
            for (Object data : dataList) {
                if (!(data instanceof AcademicSchedule schedule)) continue;
                String content = schedule.getContent();
                if (content == null || content.isBlank()) continue;
                if (!content.contains(keyword)) continue;

                LocalDate[] scheduleRange = extractScheduleDateRange(content, LocalDate.now().getYear());
                if (scheduleRange == null) continue;

                keywordOnlyMatches.add(String.format("[%s ~ %s] %s", scheduleRange[0], scheduleRange[1], content));
            }

            if (!keywordOnlyMatches.isEmpty()) {
                return String.format("'%s' 키워드로 찾은 학사일정입니다:\n\n%s",
                        keyword, String.join("\n\n", keywordOnlyMatches));
            }
        }

        // fallback: keyword는 있지만 해당 날짜 범위에 없을 때
        if (dateFilterApplied && hasKeyword) {
            String otherDate = findKeywordInOtherDates(keyword, startDate, endDate);
            if (!otherDate.isBlank()) {
                return String.format("요청하신 기간에는 '%s' 키워드 일정이 없지만, %s에 같은 일정이 있습니다.", keyword, otherDate);
            }
        }

        // 날짜 기반 응답
        if (dateFilterApplied) {
            if (!foundDateInRange) {
                return String.format("요청하신 기간(%s ~ %s)에는 학사일정이 없어요. 다른 기간으로 다시 질문해 보시겠어요?", startDate, endDate);
            }
            if (hasKeyword) {
                return String.format("요청하신 기간에 학사일정은 있지만, '%s' 키워드를 포함한 내용은 보이지 않아요.", keyword);
            }
            return String.format("요청하신 기간(%s ~ %s)에는 조건에 맞는 학사일정을 찾지 못했어요.", startDate, endDate);
        }

        return "최근 관련 학사일정을 찾지 못했어요.";
    }

    public String findKeywordInOtherDates(String keyword, LocalDate startDate, LocalDate endDate) {
        List<AcademicSchedule> schedules = academicScheduleRepository.findByContentContaining(keyword);
        int currentYear = LocalDate.now().getYear();
        List<String> otherMatches = new ArrayList<>();

        for (AcademicSchedule schedule : schedules) {
            String content = schedule.getContent();
            if (content == null || content.isBlank()) continue;

            LocalDate[] scheduleRange = extractScheduleDateRange(content, currentYear);
            if (scheduleRange == null) continue;

            LocalDate scheduleStart = scheduleRange[0];
            LocalDate scheduleEnd = scheduleRange[1];

            // 날짜가 지정된 경우: 요청 기간 외의 일정만 수집
            if (startDate != null && endDate != null) {
                boolean isOutsideRequestedPeriod = scheduleEnd.isBefore(startDate) || scheduleStart.isAfter(endDate);
                if (isOutsideRequestedPeriod) {
                    otherMatches.add(String.format("[%s ~ %s] %s", scheduleStart, scheduleEnd, content));
                }
            } else {
                // 날짜 지정이 없는 경우: 향후 일정만 수집
                if (scheduleEnd.isAfter(LocalDate.now())) {
                    otherMatches.add(String.format("[%s ~ %s] %s", scheduleStart, scheduleEnd, content));
                }
            }
        }

        if (otherMatches.isEmpty()) return "";

        return String.format("다른 기간에 '%s' 키워드와 관련된 일정이 있어요:\n\n%s", keyword, String.join("\n\n", otherMatches));
    }

    private String extractTimeLabel(String mealTime) {
        // ex: "건강한끼(11:30~13:30)" → "건강한끼"
        int idx = mealTime.indexOf('(');
        return idx != -1 ? mealTime.substring(0, idx).trim() : mealTime.trim();
    }

    private String extractTimeRange(String mealTime) {
        int start = mealTime.indexOf("(");
        int end = mealTime.indexOf(")");
        return (start != -1 && end != -1) ? mealTime.substring(start + 1, end).trim() : "";
    }

    private String normalizeKorean(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "").replaceAll("\\u200B", "").trim();
    }
}