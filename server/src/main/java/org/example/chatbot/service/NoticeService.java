package org.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatbot.dto.NoticeDto;
import org.example.chatbot.domain.AcademicNotice;
import org.example.chatbot.domain.HankyongNotice;
import org.example.chatbot.domain.ScholarshipNotice;
import org.example.chatbot.repository.AcademicNoticeRepository;
import org.example.chatbot.repository.HankyongNoticeRepository;
import org.example.chatbot.repository.ScholarshipNoticeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final AcademicNoticeRepository academicNoticeRepository;
    private final ScholarshipNoticeRepository scholarshipNoticeRepository;
    private final HankyongNoticeRepository hankyongNoticeRepository;

    /**
     * 공지사항 검색 (intent + keyword + 날짜 범위)
     */
    public List<NoticeDto> searchNotices(String intent, String keyword, LocalDate startDate, LocalDate endDate) {
        keyword = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);

        List<NoticeDto> result = new ArrayList<>();

        switch (intent) {
            case "학사공지" -> result.addAll(
                    filterNoticeByDateAndKeyword(academicNoticeRepository.findAll(), keyword, startDate, endDate)
            );
            case "장학공지" -> result.addAll(
                    filterNoticeByDateAndKeyword(scholarshipNoticeRepository.findAll(), keyword, startDate, endDate)
            );
            case "한경공지" -> result.addAll(
                    filterNoticeByDateAndKeyword(hankyongNoticeRepository.findAll(), keyword, startDate, endDate)
            );
            case "전체공지" -> {
                result.addAll(filterNoticeByDateAndKeyword(academicNoticeRepository.findAll(), keyword, startDate, endDate));
                result.addAll(filterNoticeByDateAndKeyword(scholarshipNoticeRepository.findAll(), keyword, startDate, endDate));
                result.addAll(filterNoticeByDateAndKeyword(hankyongNoticeRepository.findAll(), keyword, startDate, endDate));
            }
            default -> {
                log.warn("지원하지 않는 intent: {}", intent);
                return List.of();
            }
        }

        return result;
    }

    /**
     * 날짜 및 키워드 필터링
     */
    private List<NoticeDto> filterNoticeByDateAndKeyword(List<? extends Object> notices,
                                                         String keyword,
                                                         LocalDate startDate,
                                                         LocalDate endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return notices.stream()
                .map(this::mapToDtoWithDate)
                .filter(dto -> containsKeyword(dto.getTitle(), keyword))
                .filter(dto -> {
                    try {
                        LocalDate date = LocalDate.parse(dto.getDate(), formatter);
                        return (date.isEqual(startDate) || date.isAfter(startDate))
                                && (date.isEqual(endDate) || date.isBefore(endDate));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 제목에 키워드 포함 여부
     */
    private boolean containsKeyword(String title, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String lowerTitle = title.toLowerCase(Locale.ROOT);
        return lowerTitle.contains(keyword)
                || lowerTitle.contains("[" + keyword + "]");
    }

    /**
     * 공지 타입별 DTO 변환 (날짜 포함)
     */
    private NoticeDto mapToDtoWithDate(Object notice) {
        if (notice instanceof AcademicNotice an) {
            return new NoticeDto("학사공지", an.getTitle(), an.getLink(), an.getNoticeDate());
        } else if (notice instanceof ScholarshipNotice sn) {
            return new NoticeDto("장학공지", sn.getTitle(), sn.getLink(), sn.getNoticeDate());
        } else if (notice instanceof HankyongNotice hn) {
            return new NoticeDto("한경공지", hn.getTitle(), hn.getLink(), hn.getNoticeDate());
        } else {
            throw new IllegalArgumentException("지원하지 않는 공지 유형입니다: " + notice);
        }
    }
}