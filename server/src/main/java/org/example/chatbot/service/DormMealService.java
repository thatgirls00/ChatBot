package org.example.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.example.chatbot.domain.DormMeal;
import org.example.chatbot.repository.DormMealRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DormMealService extends AbstractSearchService<DormMeal> {

    private final DormMealRepository repository;

    @Override
    @Cacheable(value = "dormMeals", key = "{#date, #keyword}")
    public List<DormMeal> search(String date, String keyword) {
        return super.search(date, keyword);
    }

    @CacheEvict(value = "dormMeals", allEntries = true)
    public void clearCache() {
    }

    @Override
    protected List<DormMeal> findByDateAndKeyword(String date, String keyword) {
        return repository.findByMealDateContainingAndMenuContaining(date, keyword);
    }

    @Override
    protected List<DormMeal> findByDate(String date) {
        return repository.findByMealDateContaining(date);
    }

    @Override
    protected List<DormMeal> findByKeyword(String keyword) {
        return repository.findByMenuContaining(keyword);
    }

    @Override
    protected List<DormMeal> findAll() {
        return repository.findAll();
    }
}