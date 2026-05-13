package com.atguigu.exam.service;


import com.atguigu.exam.entity.Question;
import com.atguigu.exam.vo.AiGenerateRequestVo;
import com.atguigu.exam.vo.GradingResult;
import com.atguigu.exam.vo.QuestionImportVo;

import java.util.List;

/**
 * deepseek AI服务接口
 * 用于调用deepseek API生成题目
 */
public interface DeepseekAiService {
    /**
     *
     *根据前端发过来的数据拼接提示词
     * @param request
     * @return {@link String }
     */
    String buildPrompt(AiGenerateRequestVo request);

    /**
     * ai生成题目
     * @param request
     * @return
     */
    List<QuestionImportVo> aiGenerateQuestions(AiGenerateRequestVo request) throws InterruptedException;
    /**
     * 生成ai评语
     * @param totalScore
     * @param maxScore
     * @param questionCount
     * @param correctCount
     * @return
     */
    String buildSummary(Integer totalScore, Integer maxScore, Integer questionCount, Integer correctCount) throws InterruptedException;


    /**
     * 使用ai,进行简答题判断
     * @param question
     * @param userAnswer
     * @param maxScore
     * @return
     */
    GradingResult gradingTextQuestion(Question question, String userAnswer, Integer maxScore) throws InterruptedException;
}