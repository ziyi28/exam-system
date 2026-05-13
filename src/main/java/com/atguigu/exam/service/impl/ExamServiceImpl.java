package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.AnswerRecord;
import com.atguigu.exam.entity.ExamRecord;
import com.atguigu.exam.entity.Paper;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.mapper.ExamRecordMapper;
import com.atguigu.exam.service.AnswerRecordService;
import com.atguigu.exam.service.DeepseekAiService;
import com.atguigu.exam.service.ExamService;
import com.atguigu.exam.service.PaperService;
import com.atguigu.exam.vo.ExamRankingVO;
import com.atguigu.exam.vo.GradingResult;
import com.atguigu.exam.vo.StartExamVo;
import com.atguigu.exam.vo.SubmitAnswerVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 考试服务实现类
 */
@Service
@Slf4j
public class ExamServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamService {

    @Autowired
    PaperService paperService;
    @Autowired
    AnswerRecordService answerRecordService;
    @Autowired
    DeepseekAiService deepseekAiService;
    @Autowired
    ExamRecordMapper examRecordMapper;


    @Override
    public ExamRecord startExam(StartExamVo startExamVo) {
        LambdaQueryWrapper<ExamRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExamRecord::getExamId, startExamVo.getPaperId())
                .eq(ExamRecord::getStudentName, startExamVo.getStudentName())
                .eq(ExamRecord::getStatus, "进行中");
        ExamRecord examRecord = getOne(queryWrapper);
        if (examRecord != null) {
            log.debug("考生{}下{}试卷有一厂正在进行中的考试，直接返回！", examRecord.getStudentName(), examRecord.getExamId());
            return examRecord;
        }


        examRecord = new ExamRecord();
        examRecord.setExamId(startExamVo.getPaperId());
        examRecord.setStudentName(startExamVo.getStudentName());
        examRecord.setStartTime(LocalDateTime.now());
        examRecord.setWindowSwitches(0);
        examRecord.setStatus("进行中");
        save(examRecord);

        return examRecord;
    }

    @Override
    public ExamRecord customDetailExamById(Integer id) {
        // 1.查询对应考试记录表
        ExamRecord examRecord = getById(id);
        if (examRecord == null) {
            throw new RuntimeException("该考试记录已经被删除！！");
        }
        // 2.获取对应试卷详情
        Paper paper = paperService.customPaperDetailById(examRecord.getExamId());
        if (ObjectUtils.isEmpty(paper)) {
            throw new RuntimeException("改考试记录下的试卷已经被删除，无法获取详情！！");
        }
        examRecord.setPaper(paper);
        // 3.获取答题记录，并且按照题目顺序排序
        LambdaQueryWrapper<AnswerRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AnswerRecord::getExamRecordId, id);
        List<AnswerRecord> answerRecords = answerRecordService.list(queryWrapper);
        if (!ObjectUtils.isEmpty(answerRecords)) {
            List<Question> questions = paper.getQuestions();
            List<AnswerRecord> answerRecordList = answerRecords.stream().sorted((o1, o2) -> {
                int x = questions.indexOf(o1.getQuestionId());
                int y = questions.indexOf(o2.getQuestionId());
                return Integer.compare(x, y);
            }).collect(Collectors.toList());
            examRecord.setAnswerRecords(answerRecordList);
        }


        return examRecord;
    }

    @Override
    public void submitExamAnswer(Integer examRecordId, List<SubmitAnswerVo> answers) throws InterruptedException {
        // 更新考试记录状态，修改时间
        ExamRecord examRecord = getById(examRecordId.longValue());
        examRecord.setEndTime(LocalDateTime.now());
        examRecord.setStatus("已完成");
        updateById(examRecord);

        // 保存答案记录
        if (!ObjectUtils.isEmpty(answers)) {
            List<AnswerRecord> answerRecordList = answers.stream().map(sub -> new AnswerRecord(examRecordId, sub.getQuestionId(), sub.getUserAnswer()))
                    .collect(Collectors.toList());
            answerRecordService.saveBatch(answerRecordList);
        }
        // 调用判卷方法
        gradeExam(examRecordId);


    }

    public ExamRecord gradeExam(Integer examRecordId) throws InterruptedException {
        // 获取试卷详细信息，判卷，评语
        ExamRecord examRecord = customDetailExamById(examRecordId);
        Paper paper = examRecord.getPaper();
        if (paper == null) {
            examRecord.setStatus("已批阅");
            examRecord.setAnswers("改试卷已经被删除，无法批阅");
            updateById(examRecord);
            throw new RuntimeException("该试卷已经被删除，无法批阅");
        }
        List<AnswerRecord> answerRecords = examRecord.getAnswerRecords();
        if (ObjectUtils.isEmpty(answerRecords)) {
            examRecord.setAnswers("学生直接弃卷，没有答题，态度恶劣");
            examRecord.setScore(0);
            examRecord.setStatus("已批阅");
            updateById(examRecord);
            return examRecord;

        }
        // 进行判卷
        Integer totalScore = 0;
        Integer correctCount = 0;
        List<Question> questions = paper.getQuestions();
        Map<Long, Question> questionMap = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));
        // 中间报错继续判题
        for (AnswerRecord answerRecord : answerRecords) {
            try {
                Question question = questionMap.get(answerRecord.getQuestionId().longValue());
                String systemAnswer = question.getAnswer().getAnswer();
                String userAnswer = answerRecord.getUserAnswer();
                if (!"TEXT".equalsIgnoreCase(question.getType())) {
                    if ("JUDGE".equalsIgnoreCase(question.getType())) {
                        userAnswer = normalizeJudgeAnswer(userAnswer);
                    }
                    if (systemAnswer.equalsIgnoreCase(userAnswer)) {
                        answerRecord.setScore(question.getPaperScore().intValue());
                        answerRecord.setIsCorrect(1);
                    } else {
                        answerRecord.setScore(0);
                        answerRecord.setIsCorrect(0);
                    }

                } else {
                    // ai判卷
                    GradingResult gradingResult = deepseekAiService.gradingTextQuestion(question, userAnswer, question.getPaperScore().intValue());
                    answerRecord.setScore(gradingResult.getScore());
                    if (gradingResult.getScore() == 0) {
                        answerRecord.setIsCorrect(0);
                        answerRecord.setAiCorrection(gradingResult.getReason());

                    } else if (gradingResult.getScore() == question.getPaperScore().intValue()) {
                        answerRecord.setIsCorrect(1);
                        answerRecord.setAiCorrection(gradingResult.getFeedback());


                    } else {
                        answerRecord.setIsCorrect(2);
                        answerRecord.setAiCorrection(gradingResult.getReason());

                    }

                }


            } catch (Exception e) {
                e.printStackTrace();

                answerRecord.setIsCorrect(0);
                answerRecord.setScore(0);
                answerRecord.setAiCorrection("判题过程出错");


            }
            totalScore += answerRecord.getScore();
            if (answerRecord.getIsCorrect() == 1) {
                correctCount++;
            }


        }
        answerRecordService.updateBatchById(answerRecords);
        examRecord.setScore(totalScore);

        String summary = deepseekAiService.buildSummary(totalScore, paper.getTotalScore().intValue(), paper.getQuestionCount(), correctCount);

        examRecord.setAnswers(summary);
        examRecord.setScore(totalScore);
        examRecord.setStatus("已批阅");
        updateById(examRecord);

        return examRecord;


    }

    @Override
    public List<ExamRankingVO> customGetRanking(Integer paperId, Integer limit) {
        return examRecordMapper.customRankingLimit(paperId,limit);
    }

    private String normalizeJudgeAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return "";
        }

        String normalized = answer.trim().toUpperCase();
        switch (normalized) {
            case "T":
            case "TRUE":
            case "正确":
                return "TRUE";
            case "F":
            case "FALSE":
            case "错":
                return "FALSE";
            default:
                return normalized;
        }
    }
}