package org.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.StudentMeal;
import org.example.chatbot.repository.StudentMealRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentMealService extends AbstractSearchService<StudentMeal> {

    private final StudentMealRepository repository;

    @Override
    @Cacheable(value = "studentMeals", key = "{#date, #keyword}")
    public List<StudentMeal> search(String date, String keyword) {
        return super.search(date, keyword);
    }

    @CacheEvict(value = "studentMeals", allEntries = true)
    public void clearCache() {
    }

    @Override
    protected List<StudentMeal> findByDateAndKeyword(String date, String keyword) {
        return repository.findByMealDateContainingAndMenuContaining(date, keyword);
    }

    @Override
    protected List<StudentMeal> findByDate(String date) {
        return repository.findByMealDateContaining(date);
    }

    @Override
    protected List<StudentMeal> findByKeyword(String keyword) {
        return repository.findByMenuContaining(keyword);
    }

    @Override
    protected List<StudentMeal> findAll() {
        return repository.findAll();
    }
}