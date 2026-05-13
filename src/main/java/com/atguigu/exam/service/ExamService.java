package com.atguigu.exam.service;

import com.atguigu.exam.entity.ExamRecord;
import com.atguigu.exam.vo.ExamRankingVO;
import com.atguigu.exam.vo.StartExamVo;
import com.atguigu.exam.vo.SubmitAnswerVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 考试服务接口
 */
public interface ExamService extends IService<ExamRecord> {

    /**
     * 记录考试记录
     * @param startExamVo
     * @return {@link ExamRecord }
     */
    ExamRecord startExam(StartExamVo startExamVo);

    /**
     * 查询考试记录详情
     * @param id
     * @return {@link ExamRecord }
     */
    ExamRecord customDetailExamById(Integer id);

    /**
     * 提交考试答案
     * @param examRecordId
     * @param answers
     */
    void submitExamAnswer(Integer examRecordId, List<SubmitAnswerVo> answers) throws InterruptedException;

    ExamRecord gradeExam(Integer examRecordId) throws InterruptedException;

    List<ExamRankingVO> customGetRanking(Integer paperId, Integer limit);
}
 