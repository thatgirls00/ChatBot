package org.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.AcademicSchedule;
import org.example.chatbot.repository.AcademicScheduleRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicScheduleService extends AbstractSearchService<AcademicSchedule> {

    private final AcademicScheduleRepository repository;

    @Override
    @Cacheable(value = "academicSchedules", key = "{#date, #keyword}")
    public List<AcademicSchedule> search(String date, String keyword) {
        return super.search(date, keyword);
    }

    @CacheEvict(value = "academicSchedules", allEntries = true)
    public void clearCache() {
    }

    @Override
    protected List<AcademicSchedule> findByDateAndKeyword(String date, String keyword) {
        return repository.findByDateContainingAndContentContaining(date, keyword);
    }

    @Override
    protected List<AcademicSchedule> findByDate(String date) {
        return repository.findByDateContaining(date);
    }

    @Override
    protected List<AcademicSchedule> findByKeyword(String keyword) {
        return repository.findByContentContaining(keyword);
    }

    @Override
    protected List<AcademicSchedule> findAll() {
        return repository.findAll();
    }
}