package com.atguigu.exam.service;

import com.atguigu.exam.entity.ExamRecord;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;


import java.util.List;

/**
 * 考试记录Service接口
 * 定义考试记录相关的业务方法
 */
public interface ExamRecordService extends IService<ExamRecord> {

    void getDetailExamRecordList(Page<ExamRecord> mypage, LambdaQueryWrapper<ExamRecord> queryWrapper);

    void removeRecordById(Integer id);
}