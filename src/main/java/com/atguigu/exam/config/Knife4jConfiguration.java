package com.atguigu.exam.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * projectName: com.atguigu.exam.config
 *
 * @author: ziyi
 * description:
 */
@Configuration
public class Knife4jConfiguration {


    /**
     * 配置OpenAPI文档信息
     * 设置API文档的标题、描述、版本等基本信息
     * Knife4j会自动读取这些信息并生成美观的文档页面
     *
     * @return OpenAPI配置对象
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("🎓 智能考试系统API文档") // API文档标题，添加emoji图标
                        .description("📚 智能考试系统后端接口文档，提供完整的RESTful API服务\n\n" +
                                "✨ 主要功能模块：\n" +
                                "• 🧠 题目管理：支持选择题、判断题、简答题的增删改查\n" +
                                "• 📝 试卷管理：手动组卷和AI智能组卷\n" +
                                "• 🎨 轮播图管理：首页轮播图的图片上传和管理\n" +
                                "• 📊 考试记录：考试结果统计和分析\n" +
                                "• 🔔 公告管理：系统公告的发布和管理") // API文档描述，使用markdown格式
                        .version("v1.0.0"));
    }


    //1.用户管理两套结构 【实现】
    @Bean
    public GroupedOpenApi userAPI() {

        return GroupedOpenApi.builder().group("用户信息管理").
                pathsToMatch(
                        "/api/user/**"
                ).
                build();
    }

    //2.试题信息管理5+16 = 21套接口 【实现】
    @Bean
    public GroupedOpenApi questionsAPI() {

        return GroupedOpenApi.builder().group("试题信息管理").
                pathsToMatch(
                        "/api/categories/**",
                        "/api/questions/**"
                ).
                build();
    }



    //3.试卷信息管理7+5+4=16套接口 【实现】
    @Bean
    public GroupedOpenApi papersAPI() {

        return GroupedOpenApi.builder().group("考试信息管理").
                pathsToMatch(
                        "/api/papers/**",
                        "/api/exams/**",
                        "/api/exam-records/**"
                ).
                build();
    }

    //4.视频信息管理（管理端视频，客户端视频，视频分类）8+7+7=22套接口 【不实现】
    @Bean
    public GroupedOpenApi videosAPI() {

        return GroupedOpenApi.builder().group("视频信息管理").
                pathsToMatch(
                        "/api/admin/videos/**",
                        "/api/videos/**",
                        "/api/video-categories/**"
                ).
                build();
    }


    //5.系统信息管理（视频和轮播图）十六套接口 【实现轮播图，讲解文件上传】
    @Bean
    public GroupedOpenApi systemAPI() {

        return GroupedOpenApi.builder().group("系统信息管理").
                pathsToMatch(
                        "/api/banners/**",
                        "/api/notices/**"
                ).
                build();
    }


    //6.其他信息管理（数据统计，文件查询，debug）四套接口 【实现文件查询】
    @Bean
    public GroupedOpenApi otherAPI() {

        return GroupedOpenApi.builder().group("其他内容管理").
                pathsToMatch(
                        "/api/stats/**",
                        "/files/**",
                        "/api/debug/**"
                ).
                build();
    }


}