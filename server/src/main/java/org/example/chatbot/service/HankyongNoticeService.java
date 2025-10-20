package org.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.HankyongNotice;
import org.example.chatbot.repository.HankyongNoticeRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HankyongNoticeService extends AbstractSearchService<HankyongNotice> {

    private final HankyongNoticeRepository repository;

    @Override
    @Cacheable(value = "hankyongNotices", key = "{#date, #keyword}")
    public List<HankyongNotice> search(String date, String keyword) {
        return super.search(date, keyword);
    }

    @CacheEvict(value = "hankyongNotices", allEntries = true)
    public void clearCache() {
    }

    @Override
    protected List<HankyongNotice> findByDateAndKeyword(String date, String keyword) {
        return repository.findByNoticeDateContainingAndTitleContaining(date, keyword);
    }

    @Override
    protected List<HankyongNotice> findByDate(String date) {
        return repository.findByNoticeDateContaining(date);
    }

    @Override
    protected List<HankyongNotice> findByKeyword(String keyword) {
        return repository.findByTitleContaining(keyword);
    }

    @Override
    protected List<HankyongNotice> findAll() {
        return repository.findAll();
    }
}