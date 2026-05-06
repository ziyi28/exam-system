package com.atguigu.exam.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * projectName: day23_exam-system-server
 *
 * @author: 赵伟风
 * description: mybatis-plus专属的配置类
 */
@Configuration
@MapperScan("com.atguigu.exam.mapper")
public class MybatisPlusConfiguration {
}
