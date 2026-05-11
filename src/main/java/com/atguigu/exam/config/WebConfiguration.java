package com.atguigu.exam.config;

import com.atguigu.exam.config.properties.DeepseekProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @Author: ziyi
 * @Date: 2026/5/11 17:05
 * @Version: v1.0.0
 * @Description: TODO 配置webclient基本参数并加入到容器中
 **/
@Configuration
@EnableConfigurationProperties(DeepseekProperties.class)
public class WebConfiguration {
    @Autowired
    DeepseekProperties deepseekProperties;
    @Bean
    public WebClient deepseekWebClient(){

        return WebClient.builder()
                .baseUrl(deepseekProperties.getUri()+"/v1/chat/completions")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Bearer " + deepseekProperties.getApiKey())
                .build();
    }
}
