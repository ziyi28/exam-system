package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.Question;
import com.atguigu.exam.entity.QuestionAnswer;
import com.atguigu.exam.entity.QuestionChoice;
import com.atguigu.exam.mapper.QuestionAnswerMapper;
import com.atguigu.exam.mapper.QuestionChoiceMapper;
import com.atguigu.exam.mapper.QuestionMapper;
import com.atguigu.exam.service.QuestionService;
import com.atguigu.exam.vo.QuestionPageVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 题目Service实现类
 * 实现题目相关的业务逻辑
 */
@Slf4j
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Autowired
    QuestionAnswerMapper questionAnswerMapper;
    @Autowired
    QuestionChoiceMapper questionChoiceMapper;
    @Override
    public void customPageService(IPage<Question> customPage, QuestionPageVo questionPageVo) {
        questionAnswerMapper.customPage(customPage,questionPageVo);
    }

    @Override
    public void customPageServiceForJava(IPage<Question> customPage, QuestionPageVo questionPageVo) {
        //1.判断非空，书写查询条件
        LambdaQueryWrapper<Question> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(!ObjectUtils.isEmpty(questionPageVo.getCategoryId()),Question::getCategoryId,questionPageVo.getCategoryId());
        lambdaQueryWrapper.eq(!ObjectUtils.isEmpty(questionPageVo.getDifficulty()),Question::getDifficulty,questionPageVo.getDifficulty());
        lambdaQueryWrapper.eq(!ObjectUtils.isEmpty(questionPageVo.getType()),Question::getType,questionPageVo.getType());
        lambdaQueryWrapper.like(!ObjectUtils.isEmpty(questionPageVo.getKeyword()),Question::getTitle,questionPageVo.getKeyword());
        //2.分页查询出符合条件的题目
        page(customPage, lambdaQueryWrapper);
        fillQuestionChoiceAndAnswer(customPage.getRecords());
}

    /**
     *填充选项和答案到集合中
     *
     * @param records
     */
    private void fillQuestionChoiceAndAnswer(List<Question> records) {
        //转成map集合
        Map<Long, Question> questionMap = records.stream().collect(Collectors.toMap(Question::getId, q -> q));
        //查询id对应的答案
        List<Long> ids = records.stream().map(Question::getId).collect(Collectors.toList());
        List<QuestionAnswer> questionAnswers = questionAnswerMapper.selectBatchIds(ids);
        //查询id对应选项
        List<QuestionChoice> questionChoices = questionChoiceMapper.selectBatchIds(ids);
        //更具问题id分组
        Map<Long, List<QuestionChoice>> choiceMap = questionChoices.stream().collect(Collectors.groupingBy(QuestionChoice::getQuestionId));
        Map<Long, QuestionAnswer> answerMap = questionAnswers.stream().collect(Collectors.toMap(QuestionAnswer::getQuestionId, q -> q));
        //填充
        records.forEach(question -> {
            //选择题才有选项
            if ("CHOICE".equals(question.getType())){
                List<QuestionChoice> choiceList = choiceMap.getOrDefault(question.getId(), new ArrayList<>());
                choiceList.sort(Comparator.comparingInt(QuestionChoice::getSort));
                question.setChoices(choiceList);
            }

            question.setAnswer(answerMap.get(question.getId()));
        });
    }
    }