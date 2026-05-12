package com.atguigu.exam.service;

import com.atguigu.exam.entity.Paper;
import com.atguigu.exam.vo.AiPaperVo;
import com.atguigu.exam.vo.PaperVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 试卷服务接口
 */
public interface PaperService extends IService<Paper> {


    /**
     * 查询试卷详情
     * @param id
     * @return {@link Paper }
     */
    Paper customPaperDetailById(Integer id);

    /**
     * 手动组卷
     * @param paperVo
     * @return {@link Paper }
     */
    Paper createTestPapersManually(PaperVo paperVo);

    /**
     * 根据规则智能组卷
     * @param aiPaperVo
     * @return {@link Paper }
     */
    Paper intelligentVolume(AiPaperVo aiPaperVo);

    /**
     * 修改试卷内容
     * @param id
     * @param paperVo
     * @return {@link Paper }
     */
    Paper customUpdatePaperById(Integer id, PaperVo paperVo);

    /**
     * 更新试卷状态
     * @param id
     * @param status
     */
    void updatePaperStatus(Integer id, String status);

    /**删除试卷
     * @param id
     */
    void removePaperById(Integer id);
}
