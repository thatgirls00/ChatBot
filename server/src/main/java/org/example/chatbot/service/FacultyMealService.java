package org.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.FacultyMeal;
import org.example.chatbot.repository.FacultyMealRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FacultyMealService extends AbstractSearchService<FacultyMeal> {

    private final FacultyMealRepository repository;

    @Override
    @Cacheable(value = "facultyMeals", key = "{#date, #keyword}")
    public List<FacultyMeal> search(String date, String keyword) {
        return super.search(date, keyword);
    }

    @CacheEvict(value = "facultyMeals", allEntries = true)
    public void clearCache() {
    }

    @Override
    protected List<FacultyMeal> findByDateAndKeyword(String date, String keyword) {
        return repository.findByMealDateContainingAndMenuContaining(date, keyword);
    }

    @Override
    protected List<FacultyMeal> findByDate(String date) {
        return repository.findByMealDateContaining(date);
    }

    @Override
    protected List<FacultyMeal> findByKeyword(String keyword) {
        return repository.findByMenuContaining(keyword);
    }

    @Override
    protected List<FacultyMeal> findAll() {
        return repository.findAll();
    }
}