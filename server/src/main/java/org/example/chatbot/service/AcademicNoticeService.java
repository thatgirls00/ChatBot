package org.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.AcademicNotice;
import org.example.chatbot.repository.AcademicNoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicNoticeService extends AbstractSearchService<AcademicNotice> {

    private final AcademicNoticeRepository repository;

    @Override
    @Cacheable(value = "academicNotices", key = "{#date, #keyword}")
    public List<AcademicNotice> search(String date, String keyword) {
        return super.search(date, keyword);
    }

    @CacheEvict(value = "academicNotices", allEntries = true)
    public void clearCache() {
    }

    @Override
    protected List<AcademicNotice> findByDateAndKeyword(String date, String keyword) {
        return repository.findByNoticeDateContainingAndTitleContaining(date, keyword);
    }

    @Override
    protected List<AcademicNotice> findByDate(String date) {
        return repository.findByNoticeDateContaining(date);
    }

    @Override
    protected List<AcademicNotice> findByKeyword(String keyword) {
        return repository.findByTitleContaining(keyword);
    }

    @Override
    protected List<AcademicNotice> findAll() {
        return repository.findAll();
    }
}