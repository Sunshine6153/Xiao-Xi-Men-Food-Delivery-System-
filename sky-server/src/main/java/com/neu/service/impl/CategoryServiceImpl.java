package com.neu.service.impl;

import com.neu.dto.CategoryDTO;
import com.neu.entity.Category;
import com.neu.exception.MerchantBusinessException;
import com.neu.mapper.CategoryMapper;
import com.neu.mapper.DishMapper;
import com.neu.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private DishMapper dishMapper;


    @Override
    public List<Category> list() {
        return categoryMapper.list();
    }

    @Override
    public List<Category> listForUser() {
        return categoryMapper.listForUser();
    }

    @Override
    public Category getById(Long id) {
        Category category = categoryMapper.getById(id);
        if (category == null) {
            throw new MerchantBusinessException(404, "菜品分类不存在");
        }
        return category;
    }

    @Override
    public void save(CategoryDTO categoryDTO) {
        Category category = Category.builder()
                .name(categoryDTO.getName())
                .sort(categoryDTO.getSort() == null ? 0 : categoryDTO.getSort())
                .status(1)
                .build();
        categoryMapper.insertCategory(category);
    }

    @Override
    public void update(CategoryDTO categoryDTO) {
        Category category = Category.builder()
                .id(categoryDTO.getId())
                .name(categoryDTO.getName())
                .sort(categoryDTO.getSort())
                .build();
        categoryMapper.updateCategory(category);
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        categoryMapper.startOrStop(Category.builder().id(id).status(status).build());
    }

    @Override
    public void delete(Long id) {
        if (categoryMapper.getById(id) == null) {
            throw new MerchantBusinessException(404, "菜品分类不存在");
        }
        if (dishMapper.countByCategoryId(id) > 0) {
            throw new MerchantBusinessException(409, "分类下存在菜品，不能删除");
        }
        categoryMapper.deleteById(id);
    }
}
