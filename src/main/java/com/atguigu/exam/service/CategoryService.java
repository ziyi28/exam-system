package com.atguigu.exam.service;

import com.atguigu.exam.entity.Category;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CategoryService extends IService<Category> {


    List<Category> getCategoryList();

    List<Category> getCategoryTreeCount();

    void saveCategory(Category category);

    void updateCategory(Category category);
}