package com.atguigu.exam.mapper;

import com.atguigu.exam.entity.Question;
import com.atguigu.exam.entity.QuestionAnswer;
import com.atguigu.exam.vo.QuestionPageVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface QuestionAnswerMapper extends BaseMapper<QuestionAnswer> {

    /**
     *分类查询题目信息，一会儿触发第二步查询题目对应答案
     *
     * @param page
     * @param questionPageVo
     * @return {@link IPage }<{@link Question }>
     */
    IPage<Question> customPage(IPage page, QuestionPageVo  questionPageVo);
} 