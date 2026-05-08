package com.atguigu.exam.mapper;


import com.atguigu.exam.entity.QuestionChoice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 题目选项
 */
public interface QuestionChoiceMapper extends BaseMapper<QuestionChoice> {

    /**
     * 根据问题id查询选项并且排序
     * @param questionId 要查询选项问题的id
     * @return
     */
    @Select("SELECT * FROM question_choices where is_deleted=0 and question_id=#{questionId} order by sort")
    List<QuestionChoice> selectListByQuestionId(Long questionId);
} 