package com.atguigu.exam.service;


import com.atguigu.exam.vo.AiGenerateRequestVo;
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
}