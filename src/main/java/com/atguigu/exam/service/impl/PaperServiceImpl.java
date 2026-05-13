package com.atguigu.exam.service.impl;


import com.atguigu.exam.entity.*;
import com.atguigu.exam.mapper.CategoryMapper;
import com.atguigu.exam.mapper.ExamRecordMapper;
import com.atguigu.exam.mapper.PaperMapper;
import com.atguigu.exam.mapper.PaperQuestionMapper;
import com.atguigu.exam.service.CategoryService;
import com.atguigu.exam.service.PaperQuestionService;
import com.atguigu.exam.service.PaperService;
import com.atguigu.exam.service.QuestionService;
import com.atguigu.exam.vo.AiPaperVo;
import com.atguigu.exam.vo.PaperVo;
import com.atguigu.exam.vo.RuleVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;


/**
 * 试卷服务实现类
 */
@Slf4j
@Service
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {
    @Autowired
    PaperMapper paperMapper;
    @Autowired
    PaperQuestionService paperQuestionService;
    @Autowired
    QuestionService questionService;
    @Autowired
    ExamRecordMapper examRecordMapper;

    @Override
    public void removePaperById(Integer id) {
        Paper paper = getById(id);
        //判断是否为空或发布状态
        if ("PUBLISHED".equals(paper.getStatus()) || paper == null){
            throw new RuntimeException("试卷%s为空或者为发布状态，删除失败".formatted(paper.getName()));
        }
        //判断是否关联答题记录表
        LambdaQueryWrapper<ExamRecord> lambdaQueryWrapper= new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ExamRecord::getExamId,id);
        Long count = examRecordMapper.selectCount(lambdaQueryWrapper);
        if (count > 0){
            throw new RuntimeException("试卷：%s下关联了%s条考试记录数据，删除失败".formatted(paper.getName(),count));
        }
        //删除试卷
        removeById(Long.valueOf(id));
        //删除中间表
        paperQuestionService.remove(new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId,id));


    }


    @Override
    public void updatePaperStatus(Integer id, String status) {
        if ("PUBLISHED".equals(status)){
            LambdaQueryWrapper<Paper> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Paper::getId, id);
            long count = count(queryWrapper);
            if (count==0){
                throw new RuntimeException("id:%s试卷下没有题目，发布状态下试卷必须有题目！！".formatted(id));
            }
        }
        LambdaUpdateWrapper<Paper> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Paper::getStatus, status);
        updateWrapper.eq(Paper::getId, id);
        update(updateWrapper);

    }

    @Override
    public Paper customPaperDetailById(Integer id) {
        // 先根据id查询出试卷
        Paper paper = getById(id);
        if (paper == null) {
            throw new RuntimeException("查询的试卷id不存在！！id为：%s".formatted(id));
        }
        // 根据id查询出题目
        List<Question> questions = paperMapper.selectQuestionByPaperId(id);
        if (ObjectUtils.isEmpty(questions)) {
            paper.setQuestions(new ArrayList<>());
            log.warn("试卷中没有题目！可以进行试卷编辑！但是不能用于考试！，对应试卷id{}", id);
            return paper;

        }
        // 给试卷题目排序
        log.debug("试卷题目排序前：{}", questions);
        questions.sort((o1, o2) -> Integer.compare(typeToInt(o1.getType()), typeToInt(o2.getType())));
        log.debug("试卷题目排序后：{}", questions);

        paper.setQuestions(questions);


        return paper;
    }

    private int typeToInt(String type) {
        switch (type) {
            case "CHOICE":
                return 1;
            case "JUDGE":
                return 2;
            case "TEXT":
                return 3;
            default:
                return 4;
        }
    }

    @Override
    public Paper createTestPapersManually(PaperVo paperVo) {
        Paper paper = new Paper();
        BeanUtils.copyProperties(paperVo, paper);
        paper.setStatus("DRAFT");
        Map<Integer, BigDecimal> questions = paperVo.getQuestions();
        if (ObjectUtils.isEmpty(questions)) {
            paper.setTotalScore(BigDecimal.ZERO);
            paper.setQuestionCount(0);
            log.warn("当前试卷传入题目为空，当前试卷不可以考试，试卷id为{}", paper.getId());
            return paper;
        }
        paper.setQuestionCount(questions.size());
        BigDecimal totalScore = questions.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        paper.setTotalScore(totalScore);
        save(paper);


        // 保存试卷信息和题目信息关联
        List<PaperQuestion> paperQuestionList = questions.entrySet().stream()
                .map(entry -> new PaperQuestion(paper.getId().intValue(), entry.getKey().longValue(), entry.getValue()))
                .collect(Collectors.toList());

        paperQuestionService.saveBatch(paperQuestionList);


        return paper;
    }

    @Override
    public Paper intelligentVolume(AiPaperVo aiPaperVo) {
        Paper paper = new Paper();
        BeanUtils.copyProperties(aiPaperVo, paper);
        paper.setStatus("DRAFT");
        save(paper);
        Integer totalCount = 0;
        BigDecimal totalScore = BigDecimal.ZERO;
        List<RuleVo> rules = aiPaperVo.getRules();
        for (RuleVo rule : rules) {
            if (ObjectUtils.isEmpty(rule.getCount())) {
                log.info("{}分类下不需要出题", rule.getType().name());
                continue;
            }
            // 查询对应题目
            LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(!ObjectUtils.isEmpty(rule.getCategoryIds()), Question::getCategoryId, rule.getCategoryIds())
                    .eq(Question::getType, rule.getType());
            List<Question> questionList = questionService.list(queryWrapper);
            if (ObjectUtils.isEmpty(questionList)) {
                log.warn("{}id分类下，题目类型为{}，查询出的题目集合为空", rule.getCategoryIds(), rule.getType());
                continue;
            }
            // 统计分数和数量
            int realCount = Math.min(rule.getCount(), questionList.size());
            totalCount += realCount;
            totalScore=totalScore.add(BigDecimal.valueOf(realCount * rule.getScore()));
            // 批量保存中间表
            List<PaperQuestion> paperQuestionList = questionList.stream()
                    .map(question -> new PaperQuestion(paper.getId().intValue(), question.getId(), BigDecimal.valueOf(rule.getScore())))
                    .collect(Collectors.toList());
            paperQuestionService.saveBatch(paperQuestionList);

        }
        //更新paper
        paper.setQuestionCount(totalCount);
        paper.setTotalScore(totalScore);
        updateById(paper);


        return paper;
    }

    @Override
    public Paper customUpdatePaperById(Integer id, PaperVo paperVo) {
        //先校验是否为发布状态，再校验是否重名
        Paper paper = getById(id);
        if ("PUBLISHED".equals(paper.getStatus())){
            throw new RuntimeException("试卷{}处于发布状态，不允许修改！！".formatted(paper.getName()));
        }
        LambdaQueryWrapper<Paper> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Paper::getId, id);
        queryWrapper.eq(Paper::getName, paperVo.getName());
        List<Paper> list = list(queryWrapper);
        if (!ObjectUtils.isEmpty(list)){
            throw new RuntimeException("已经有名为%s试卷存在，请修改后重试".formatted(paperVo.getName()));
        }
        //更新paper
        BeanUtils.copyProperties(paperVo, paper);
        paper.setQuestionCount(paperVo.getQuestions().size());
        BigDecimal totalScore = paperVo.getQuestions().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        paper.setTotalScore(totalScore);
        updateById(paper);
        //先删除原来的中间表
        paperQuestionService.remove(new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId,id));


        // 保存试卷信息和题目信息关联
        List<PaperQuestion> paperQuestionList = paperVo.getQuestions().entrySet().stream()
                .map(entry -> new PaperQuestion(paper.getId().intValue(), entry.getKey().longValue(), entry.getValue()))
                .collect(Collectors.toList());

        paperQuestionService.saveBatch(paperQuestionList);


        return paper;
    }
}