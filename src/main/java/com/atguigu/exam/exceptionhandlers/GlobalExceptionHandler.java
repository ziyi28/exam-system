package com.atguigu.exam.exceptionhandlers;

import com.atguigu.exam.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * projectName: day23_exam-system-server
 *
 * @author: 赵伟风
 * description: 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result exception(Exception e) {
        //记录异常日志
        log.error("服务器发生运行时异常！异常信息为：{}",e.getMessage());
        //返回对应的提示
        return Result.error(e.getMessage());
    }
}
