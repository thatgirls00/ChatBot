package org.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.ScholarshipNotice;
import org.example.chatbot.repository.ScholarshipNoticeRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScholarshipNoticeService extends AbstractSearchService<ScholarshipNotice> {

    private final ScholarshipNoticeRepository repository;

    @Override
    @Cacheable(value = "scholarshipNotices", key = "{#date, #keyword}")
    public List<ScholarshipNotice> search(String date, String keyword) {
        return super.search(date, keyword);
    }

    @CacheEvict(value = "scholarshipNotices", allEntries = true)
    public void clearCache() {
    }

    @Override
    protected List<ScholarshipNotice> findByDateAndKeyword(String date, String keyword) {
        return repository.findByNoticeDateContainingAndTitleContaining(date, keyword);
    }

    @Override
    protected List<ScholarshipNotice> findByDate(String date) {
        return repository.findByNoticeDateContaining(date);
    }

    @Override
    protected List<ScholarshipNotice> findByKeyword(String keyword) {
        return repository.findByTitleContaining(keyword);
    }

    @Override
    protected List<ScholarshipNotice> findAll() {
        return repository.findAll();
    }
}