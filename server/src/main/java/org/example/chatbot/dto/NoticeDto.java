package org.example.chatbot.dto;

import lombok.Getter;
import org.example.chatbot.domain.AcademicNotice;
import org.example.chatbot.domain.HankyongNotice;
import org.example.chatbot.domain.ScholarshipNotice;

/**
 * 공지사항 DTO
 * - type : 공지 유형 (학사 / 장학 / 한경)
 * - title : 공지 제목
 * - date : 공지 날짜 (yyyy-MM-dd)
 * - url : 공지 링크
 */
@Getter
public class NoticeDto {

    private final String type;
    private final String title;
    private final String date;
    private final String url;

    public NoticeDto(String type, String title, String date, String url) {
        this.type = type;
        this.title = title;
        this.date = date;
        this.url = url;
    }

    public static NoticeDto from(AcademicNotice notice) {
        return new NoticeDto(
                "학사공지",
                notice.getTitle(),
                notice.getNoticeDate(),
                notice.getLink()
        );
    }

    public static NoticeDto from(ScholarshipNotice notice) {
        return new NoticeDto(
                "장학공지",
                notice.getTitle(),
                notice.getNoticeDate(),
                notice.getLink()
        );
    }

    public static NoticeDto from(HankyongNotice notice) {
        return new NoticeDto(
                "한경공지",
                notice.getTitle(),
                notice.getNoticeDate(),
                notice.getLink()
        );
    }
}