package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.AnswerRecord;
import com.atguigu.exam.entity.ExamRecord;
import com.atguigu.exam.entity.Paper;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.mapper.ExamRecordMapper;
import com.atguigu.exam.service.AnswerRecordService;
import com.atguigu.exam.service.ExamService;
import com.atguigu.exam.service.PaperService;
import com.atguigu.exam.vo.StartExamVo;
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
}