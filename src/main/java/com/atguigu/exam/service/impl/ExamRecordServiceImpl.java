package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.AnswerRecord;
import com.atguigu.exam.entity.ExamRecord;
import com.atguigu.exam.entity.Paper;
import com.atguigu.exam.mapper.AnswerRecordMapper;
import com.atguigu.exam.mapper.ExamRecordMapper;
import com.atguigu.exam.mapper.PaperMapper;
import com.atguigu.exam.service.AnswerRecordService;
import com.atguigu.exam.service.ExamRecordService;
import com.atguigu.exam.service.PaperService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 考试记录Service实现类
 * 实现考试记录相关的业务逻辑
 */
@Service
public class ExamRecordServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamRecordService {

    @Autowired
    PaperMapper paperMapper;
    @Autowired
    AnswerRecordService answerRecordService;


    @Override
    public void getDetailExamRecordList(Page<ExamRecord> mypage, LambdaQueryWrapper<ExamRecord> queryWrapper) {
        Page<ExamRecord> examRecordPage = page(mypage, queryWrapper);
        List<Integer> list = examRecordPage.getRecords().stream().map(examRecord -> examRecord.getExamId()).toList();
        List<Paper> papers = paperMapper.selectBatchIds(list);
        Map<Long, Paper> collect = papers.stream().collect(Collectors.toMap(Paper::getId, q -> q));
        mypage.getRecords().forEach(examRecord -> {
            Paper paper = collect.get(examRecord.getExamId().longValue());
            examRecord.setPaper(paper);
        });



    }

    @Override
    public void removeRecordById(Integer id) {
        ExamRecord examRecord = getById(id);
        if ("进行中".equals(examRecord.getStatus())) {
            throw new RuntimeException("该记录考试怎在进行中，删除失败");
        }
        removeById(id.longValue());
        answerRecordService.remove(new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getExamRecordId,id));

    }
}