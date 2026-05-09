package com.atguigu.exam.service.impl;

import com.atguigu.exam.common.CacheConstants;
import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.PaperQuestion;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.entity.QuestionAnswer;
import com.atguigu.exam.entity.QuestionChoice;
import com.atguigu.exam.mapper.PaperQuestionMapper;
import com.atguigu.exam.mapper.QuestionAnswerMapper;
import com.atguigu.exam.mapper.QuestionChoiceMapper;
import com.atguigu.exam.mapper.QuestionMapper;
import com.atguigu.exam.service.QuestionService;
import com.atguigu.exam.utils.RedisUtils;
import com.atguigu.exam.vo.QuestionPageVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

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
        lambdaQueryWrapper.orderByDesc(Question::getUpdateTime);
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

    @Override
    public Question customDetailQuestion(Long id) {
        Question question = questionMapper.customGetById(id);
        if (question==null){
            throw new RuntimeException("查询题目详情失败，题目可能已经被删除，题目id为%s".formatted(id));
        }
        new Thread(()->{
            increamentQuestion(question.getId());
        }).start();
        return question;
    }

    private void increamentQuestion(Long questionId) {
        Double score = redisUtils.zIncrementScore(CacheConstants.POPULAR_QUESTIONS_KEY,questionId,1);
        log.info("完成{}题目分数累计，累计后分数为：{}",questionId,score);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void customSaveQuestion(Question question) {
        //判断同一分类下title是否重名
        LambdaQueryWrapper<Question> lambdaQueryWrapper =
                 new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(Question::getType,question.getType())
                .eq(Question::getTitle,question.getTitle());
        QuestionMapper basemapper = getBaseMapper();
        boolean exists = basemapper.exists(lambdaQueryWrapper);
        if (exists) {
            throw new RuntimeException("保存题目信息失败，%s分类下已经有名为%stitle的%s题型".formatted(question.getCategory(),question.getTitle(),question.getType()));

        }
        //保存题目
        boolean save = save(question);
        if (!save) {
            throw new RuntimeException("保存题目信息失败");
        }
        //填充答案
        QuestionAnswer answer = question.getAnswer();
        answer.setQuestionId(question.getId());
        //判断是否为选择题
        List<QuestionChoice> choices = question.getChoices();
        StringBuilder sb = new StringBuilder();
        if ("CHOICE".equals(question.getType())){
            for (int i = 0; i < choices.size(); i++) {
                QuestionChoice questionChoice = choices.get(i);
                questionChoice.setQuestionId(question.getId());
                if (questionChoice.getIsCorrect()){
                    if (sb.length()>0){
                        sb.append(",");
                    }
                    sb.append((char)('A'+i));
                }
            }
            //填充选择题答案
            answer.setAnswer(sb.toString());
        }
        //保存答案
        questionAnswerMapper.insert(answer);
    }
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void customUpdateQuestion(Question question) {
        //判断是否重名
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Question::getTitle,question.getTitle()).ne(Question::getId,question.getId());
        QuestionMapper baseMapper = getBaseMapper();
        boolean exists = baseMapper.exists(queryWrapper);
        if (exists){
            throw new RuntimeException("更新失败，因为已经有名为%s的题目".formatted(question.getTitle()));
        }
        //更新题目信息
        boolean issuccess = updateById(question);
        if (!issuccess){
            throw new RuntimeException("修改题目%s失败".formatted(question.getId()));

        }

        QuestionAnswer answer = question.getAnswer();
        //判断是否为选择题
        if ("CHOICE".equals(question.getType())){
            //删除原来的选项
            questionChoiceMapper.delete(new LambdaQueryWrapper<QuestionChoice>().eq(QuestionChoice::getQuestionId,question.getId()));
            List<QuestionChoice> choices = question.getChoices();
            //保存新的选项
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < choices.size(); i++) {
                QuestionChoice questionChoice = choices.get(i);
                questionChoice.setQuestionId(question.getId());
                questionChoice.setId(null);
                questionChoice.setSort(i);
                questionChoice.setCreateTime(null);
                questionChoice.setUpdateTime(null);
                questionChoiceMapper.insert(questionChoice);
                if (questionChoice.getIsCorrect()){
                    if (sb.length()>0){
                        sb.append(",");
                    }
                    sb.append((char) ('A'+i));
                }
            }
            answer.setAnswer(sb.toString());


        }
        questionAnswerMapper.updateById(answer);


    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void customRemoveQuestionById(Long id) {
        //判断试题中是否包含这道题
        LambdaQueryWrapper<PaperQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaperQuestion::getQuestionId,id);
        Long l = paperQuestionMapper.selectCount(wrapper);
        if (l>0){
            throw new RuntimeException("删除id为%s的题目失败，此题被其它试卷引用了%s次".formatted(id,l));
        }
        //删除答案和题目
        boolean isdelete = removeById(id);
        if (!isdelete) {
            throw new RuntimeException("删除id为%s的题目失败".formatted(id));
        }
        LambdaQueryWrapper<QuestionAnswer> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(QuestionAnswer::getQuestionId,id);
        questionAnswerMapper.delete(lambdaQueryWrapper);
        LambdaQueryWrapper<QuestionChoice> choiceLambdaQueryWrapper = new LambdaQueryWrapper<>();
        choiceLambdaQueryWrapper.eq(QuestionChoice::getQuestionId,id);
        questionChoiceMapper.delete(choiceLambdaQueryWrapper);


    }

    @Override
    public List<Question> customPopularQuestion(Integer size) {
        //定义一个热门题目集合
        List<Question> popularQuestions = new ArrayList<>();
        if (size<=0){
            throw new RuntimeException("传入的热门数量大小为零或者有误！！");
        }
        //获取redis中的热门题目
        List<Question> listQuestion = new ArrayList<>();
        Set<ZSetOperations.TypedTuple<Object>> typedTuples = redisUtils.zReverseRangeWithScores(CacheConstants.POPULAR_QUESTIONS_KEY, 0, size - 1);
        List<Long> listids= new ArrayList<>();
         if (typedTuples!=null && typedTuples.size()>0){
            List<Long> popularListids = typedTuples.stream().sorted((o1, o2) ->
                    Integer.compare(o2.getScore().intValue(), o1.getScore().intValue())
            ).map(q -> Long.valueOf(q.getValue().toString())).collect(Collectors.toList());
            log.debug("从zset集合中查到的有序id集合为：{}",popularListids);
            listids.addAll(popularListids);
            for (Long id : popularListids) {
                Question question = getById(id);
                if (question!=null){
                    listQuestion.add(question);
                }

            }
            popularQuestions.addAll(listQuestion);
            log.debug("从redis中查出来的热题数量为{}，题目集合为{}",popularQuestions.size(),popularQuestions);
        }
        int diff=size-popularQuestions.size();
        if (diff>0){
            LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
            wrapper.notIn(Question::getId,listids).orderByDesc(Question::getCreateTime).last("limit "+diff);
            List<Question> list = list(wrapper);
            if (list!=null && list.size()>0){
            popularQuestions.addAll(list);}
        }
        //集中填充题目答案和选项
        fillQuestionChoiceAndAnswer(popularQuestions);

        return popularQuestions;
    }
}