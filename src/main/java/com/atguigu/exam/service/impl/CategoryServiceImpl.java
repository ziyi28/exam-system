package com.atguigu.exam.service.impl;


import com.atguigu.exam.entity.Category;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.mapper.CategoryMapper;
import com.atguigu.exam.mapper.QuestionMapper;
import com.atguigu.exam.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper,Category> implements CategoryService {

    @Autowired
    QuestionMapper questionMapper;
    /**
     *
     *查询题目分类及其对应题目数量
     * @return {@link List }<{@link Category }>
     */
    @Override
    public List<Category> getCategoryList() {
        //获取所有分类
        List<Category> list = list();
        //填充count
        List<Category> categories=fillCategoryCount(list);
        return categories;
    }

    private List<Category> fillCategoryCount(List<Category> list) {
        //判断集合是否为空
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("分类集合为空！！");

        }
        //查询question表获取分类对应题目数量
        List<Map<Long,Long>> list1=questionMapper.selectCategoryCount();
        //利用stream转换成map
        Map<Long, Long> resultCount = list1.stream().
                collect(Collectors.toMap(q -> q.get("category_id"),
                        q -> q.get("ct")));
        //取出count给list赋值，填充count
        list.stream()
                .forEach(category->
                        category.setCount(resultCount.getOrDefault(category.getId(),0L)));

        return list;
    }

    @Override
    public List<Category> getCategoryTreeCount() {
        //获取所有分类
        List<Category> list = list();
        //填充count
        List<Category> categories=fillCategoryCount(list);
        //根据parent_id分类
        Map<Long, List<Category>> groupedList = categories.stream().collect(Collectors.groupingBy(Category::getParentId));
        //分组出parent_id为0的
        List<Category> resultList = list.stream().filter(category -> category.getParentId() == 0L)
                .collect(Collectors.toList());
        //填充子分类到child
        resultList.stream().forEach(category ->
                {
                    List<Category> child = groupedList.getOrDefault(category.getId(), new ArrayList<>());
                    child.sort(Comparator.comparingInt(Category::getSort));
                    category.setChildren(child);
                    long sum = child.stream().mapToLong(category1 -> category1.getCount()).sum();
                    category.setCount(category.getCount()+sum);
                }
                );
        return resultList;
    }

    /**
     *
     *新增子分类
     * @param category 子分类信息
     */
    @Override
    public void saveCategory(Category category) {
        //1.判断同一父分类下是否重名
        LambdaQueryWrapper<Category> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Category::getParentId,category.getParentId())
                .eq(Category::getName,category.getName());
        CategoryMapper categoryMapper = getBaseMapper();
        boolean exists = categoryMapper.exists(lambdaQueryWrapper);
        if (exists){
            Category category1 = getById(category.getParentId());
            throw new RuntimeException("父分类：%s已经有名为%s的分类，新增子分类失败".formatted(category1.getName(),category.getName()));
        }
        save(category);


    }

    /**
     *
     *修改同一父分类下子分类信息
     * @param category 新的分类信息
     */
    @Override
    public void updateCategory(Category category) {
        //判断同一父分类下，是否和其它分类同名，且id不同
        LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Category::getParentId,category.getParentId())
                .eq(Category::getName,category.getName())
                .ne(Category::getId,category.getId());
        CategoryMapper baseMapper = getBaseMapper();
        boolean exists = baseMapper.exists(lambdaQueryWrapper);
        if (exists){
            Category parent = getById(category.getParentId());
            throw new RuntimeException("父分类%s下已有名为%s的子分类，修改分类失败！！".formatted(parent.getName(),category.getName()));
        }
        //执行修改
        updateById(category);
    }

    /**
     *删除分类信息
     *
     * @param id 要删除的分类id
     */
    @Override
    public void removeCategory(Long id) {
        //判断分类是否为顶级分类
        Category category = getById(id);
        if (category.getParentId()==0L){
            throw new RuntimeException("该分类为顶级父分类无法删除");
        }
        //判断该分类下是否有题目
        LambdaQueryWrapper<Question> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Question::getCategoryId,id);
        Long l = questionMapper.selectCount(lambdaQueryWrapper);
        if (l>0){
            throw new RuntimeException("分类%s下有%s道题目，无法删除！！".formatted(category.getName(),l));
        }
        removeById(id);
    }
}