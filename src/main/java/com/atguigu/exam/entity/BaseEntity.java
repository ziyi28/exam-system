package com.atguigu.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class BaseEntity implements Serializable {

//    @JsonProperty("hahaha")
    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    //@JsonFormat(pattern = "yyyy年MM月dd日 HH:mm:ss" ,timezone = "GMT+8")
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    //@JsonFormat(pattern = "yyyy年MM月dd日 HH:mm:ss" ,timezone = "GMT+8")
    @Schema(description = "修改时间")
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    @Schema(description = "逻辑删除")
    @TableField("is_deleted")
    @TableLogic
    @JsonIgnore // 在请求和响应体中忽略该字段
    private Byte isDeleted;

}