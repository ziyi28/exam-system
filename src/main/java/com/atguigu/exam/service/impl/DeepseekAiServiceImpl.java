package com.atguigu.exam.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.exam.config.properties.DeepseekProperties;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.service.DeepseekAiService;
import com.atguigu.exam.vo.AiGenerateRequestVo;
import com.atguigu.exam.vo.QuestionImportVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;

/**
 * deepseek AI服务实现类
 * 调用deepseek API智能生成题目
 */
@Slf4j
@Service
public class DeepseekAiServiceImpl implements DeepseekAiService {
    @Autowired
    WebClient webClient;
    @Autowired
    DeepseekProperties deepseekProperties;

    @Override
    public String buildPrompt(AiGenerateRequestVo request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("请为我生成").append(request.getCount()).append("道关于【")
                .append(request.getTopic()).append("】的题目。\n\n");

        prompt.append("要求：\n");

        // 题目类型要求
        if (request.getTypes() != null && !request.getTypes().isEmpty()) {
            List<String> typeList = Arrays.asList(request.getTypes().split(","));
            prompt.append("- 题目类型：");
            for (String type : typeList) {
                switch (type.trim()) {
                    case "CHOICE":
                        prompt.append("选择题");
                        if (request.getIncludeMultiple() != null && request.getIncludeMultiple()) {
                            prompt.append("(包含单选和多选)");
                        }
                        prompt.append(" ");
                        break;
                    case "JUDGE":
                        prompt.append("判断题（**重要：确保正确答案和错误答案的数量大致平衡，不要全部都是正确或错误**） ");
                        break;
                    case "TEXT":
                        prompt.append("简答题 ");
                        break;
                }
            }
            prompt.append("\n");
        }

        // 难度要求
        if (request.getDifficulty() != null) {
            String difficultyText = switch (request.getDifficulty()) {
                case "EASY" -> "简单";
                case "MEDIUM" -> "中等";
                case "HARD" -> "困难";
                default -> "中等";
            };
            prompt.append("- 难度等级：").append(difficultyText).append("\n");
        }

        // 额外要求
        if (request.getRequirements() != null && !request.getRequirements().isEmpty()) {
            prompt.append("- 特殊要求：").append(request.getRequirements()).append("\n");
        }

        // 判断题特别要求
        if (request.getTypes() != null && request.getTypes().contains("JUDGE")) {
            prompt.append("- **判断题特别要求**：\n");
            prompt.append("  * 确保生成的判断题中，正确答案(TRUE)和错误答案(FALSE)的数量尽量平衡\n");
            prompt.append("  * 不要所有判断题都是正确的或都是错误的\n");
            prompt.append("  * 错误的陈述应该是常见的误解或容易混淆的概念\n");
            prompt.append("  * 正确的陈述应该是重要的基础知识点\n");
        }

        prompt.append("\n请严格按照以下JSON格式返回，不要包含任何其他文字：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"questions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"title\": \"题目内容\",\n");
        prompt.append("      \"type\": \"CHOICE|JUDGE|TEXT\",\n");
        prompt.append("      \"multi\": true/false,\n");
        prompt.append("      \"difficulty\": \"EASY|MEDIUM|HARD\",\n");
        prompt.append("      \"score\": 5,\n");
        prompt.append("      \"choices\": [\n");
        prompt.append("        {\"content\": \"选项内容\", \"isCorrect\": true/false, \"sort\": 1}\n");
        prompt.append("      ],\n");
        prompt.append("      \"answer\": \"TRUE或FALSE(判断题专用)|文本答案(简答题专用)\",\n");
        prompt.append("      \"analysis\": \"题目解析\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");

        prompt.append("注意：\n");
        prompt.append("1. 选择题必须有choices数组，判断题和简答题设置answer字段\n");
        prompt.append("2. 多选题的multi字段设为true，单选题设为false\n");
        prompt.append("3. **判断题的answer字段只能是\"TRUE\"或\"FALSE\"，请确保答案分布合理**\n");
        prompt.append("4. 每道题都要有详细的解析\n");
        prompt.append("5. 题目要有实际价值，贴近实际应用场景\n");
        prompt.append("6. 严格按照JSON格式返回，确保可以正确解析\n");

        // 如果只生成判断题，额外强调答案平衡
        if (request.getTypes() != null && request.getTypes().equals("JUDGE") && request.getCount() > 1) {
            prompt.append("7. **判断题答案分布要求**：在").append(request.getCount()).append("道判断题中，");
            int halfCount = request.getCount() / 2;
            if (request.getCount() % 2 == 0) {
                prompt.append("请生成").append(halfCount).append("道正确(TRUE)和").append(halfCount).append("道错误(FALSE)的题目");
            } else {
                prompt.append("请生成约").append(halfCount).append("-").append(halfCount + 1).append("道正确(TRUE)和约").append(halfCount).append("-").append(halfCount + 1).append("道错误(FALSE)的题目");
            }
        }

        return prompt.toString();
    }

    @Override
    public List<QuestionImportVo> aiGenerateQuestions(AiGenerateRequestVo request) throws InterruptedException {
        // 判断各个字段是否为空
        if (request.getTopic() == null || request.getTypes() == null || request.getDifficulty() == null || request.getCount() == null || request.getRequirements() == null) {
            throw new RuntimeException("AI生成题目失败，参数不完整，request={}".formatted(request));
        }
        // 将request中的数据填充对应到prompt中
        String prompt = buildPrompt(request);
        // 调用deepseek模型，传入prompt，获取结果
        String content = callDeepseekAi(prompt);
        int start = content.indexOf("```json");
        int end = content.lastIndexOf("```");
        List<QuestionImportVo> questionImportVoList = new ArrayList<>();
        if (start!=-1 && end!=-1 && start<end){
            String realContent = content.substring(start + 7, end);
            JSONObject realJsonObject = JSONObject.parseObject(realContent);
            JSONArray questions = realJsonObject.getJSONArray("questions");
            for (int i = 0; i < questions.size(); i++) {
                JSONObject questionsJSONObject = questions.getJSONObject(i);
                QuestionImportVo questionImportVo = new QuestionImportVo();
                questionImportVo.setTitle(questionsJSONObject.getString("title"));
                questionImportVo.setType(questionsJSONObject.getString("type"));
                questionImportVo.setMulti(questionsJSONObject.getBoolean("multi"));
                questionImportVo.setDifficulty(questionsJSONObject.getString("difficulty"));
                questionImportVo.setScore(questionsJSONObject.getInteger("score"));
                questionImportVo.setAnalysis(questionsJSONObject.getString("analysis"));
                questionImportVo.setCategoryId(request.getCategoryId());
                if ("CHOICE".equals(questionImportVo.getType())){
                    JSONArray choices = questionsJSONObject.getJSONArray("choices");
                    List<QuestionImportVo.ChoiceImportDto> choiceImportDtoList = new ArrayList<>(choices.size());
                    for (int i1 = 0; i1 < choices.size(); i1++) {
                        JSONObject jsonObject = choices.getJSONObject(i1);
                        QuestionImportVo.ChoiceImportDto choiceImportDto = new QuestionImportVo.ChoiceImportDto();
                        choiceImportDto.setContent(jsonObject.getString("content"));
                        choiceImportDto.setIsCorrect(jsonObject.getBoolean("isCorrect"));
                        choiceImportDto.setSort(jsonObject.getInteger("sort"));
                        choiceImportDtoList.add(choiceImportDto);
                    }
                    questionImportVo.setChoices(choiceImportDtoList);



                }
                questionImportVo.setAnswer(questionsJSONObject.getString("answer"));
                questionImportVoList.add(questionImportVo);

            }
            return questionImportVoList;

        }


        throw new RuntimeException("ai生成题目json数据结构错误，无法正常解析！数据为：%s".formatted(content));
    }

    private String callDeepseekAi(String prompt) throws InterruptedException {
        // 最多尝试3次
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                //https://api-docs.deepseek.com/zh-cn/api/create-chat-completion
                Map<String,String> userMap = new HashMap<>();
                userMap.put("role","user");
                userMap.put("content",prompt);
                List<Map> messageList = new ArrayList<>();
                messageList.add(userMap);

                Map<String,Object> requestBody = new HashMap<>();
                requestBody.put("model",deepseekProperties.getModel());
                requestBody.put("messages",messageList);
                requestBody.put("temperature",deepseekProperties.getTemperature());
                requestBody.put("max_tokens",deepseekProperties.getMaxToken());

                //发起网络请求
                String result = webClient.post()
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(100))
                        .block();
                log.info("调用deepseek模型成功，prompt={},content={}", prompt, result);

                JSONObject jsonObject = JSONObject.parseObject(result);
                if (jsonObject.containsKey("error")){
                    throw new RuntimeException("调用deepseek模型失败，错误信息为："+
                            jsonObject.getJSONObject("error").getString("message"));
                }
                String content = jsonObject.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content");

                if (content==null || content.isEmpty()){
                    throw new RuntimeException("调用deepseek模型成功，但是返回内容为空");
                }
                return content;


            } catch (Exception e) {
                log.debug("第{}次尝试调用失败了！",i);
                Thread.sleep(1000);
                if (i==maxRetries){
                    throw new RuntimeException("已经重试了三次，依然失败，请稍后重试");
                }

            }
        }

        throw new RuntimeException("已经重试了三次，依然失败，请稍后重试");
    }
}