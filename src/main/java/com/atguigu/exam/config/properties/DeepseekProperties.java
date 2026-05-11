package com.atguigu.exam.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Author: ziyi
 * @Date: 2026/5/11 16:58
 * @Version: v1.0.0
 * @Description: TODO
 **/
@ConfigurationProperties(prefix = "deepseek.api")
@Data
public class DeepseekProperties {
    private String uri;
    private String model;
    private String apiKey;
    private Double temperature;
    private Integer maxToken;

}
