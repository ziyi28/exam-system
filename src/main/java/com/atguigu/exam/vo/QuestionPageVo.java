package com.atguigu.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Author: ziyi
 * @Date: 2026/5/8 16:02
 * @Version: v1.0.0
 * @Description: TODO
 **/
@Data
@Schema(description = "前端传过来条件查询的数据")
public class QuestionPageVo {
    private Long categoryId;
    private String difficulty;
    private String type;
    private String keyword;
}
