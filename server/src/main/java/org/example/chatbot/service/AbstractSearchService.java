package org.example.chatbot.service;

import java.util.List;

public abstract class AbstractSearchService<T> {

    public List<T> search(String date, String keyword) {
        if (date != null && keyword != null) {
            return findByDateAndKeyword(date, keyword);
        }

        if (date != null) {
            return findByDate(date);
        }

        if (keyword != null) {
            return findByKeyword(keyword);
        }

        return findAll();
    }

    protected abstract List<T> findByDateAndKeyword(String date, String keyword);

    protected abstract List<T> findByDate(String date);

    protected abstract List<T> findByKeyword(String keyword);

    protected abstract List<T> findAll();
}