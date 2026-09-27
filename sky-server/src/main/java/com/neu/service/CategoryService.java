package com.neu.service;

import com.neu.dto.CategoryDTO;
import com.neu.entity.Category;

import java.util.List;

public interface CategoryService {

    List<Category> list();

    List<Category> listForUser();

    Category getById(Long id);

    void save(CategoryDTO categoryDTO);

    void update(CategoryDTO categoryDTO);

    void startOrStop(Integer status, Long id);

    void delete(Long id);
}
