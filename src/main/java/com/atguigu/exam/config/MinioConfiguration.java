package com.atguigu.exam.config;

import com.atguigu.exam.config.properties.MinioProperties;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * projectName: day23_exam-system-server
 *
 * @author: 赵伟风
 * description: Minio的配置类，完成MinioClient组件的创建
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfiguration {

    @Autowired
    private MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient() {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getUsername(), minioProperties.getPassword())
                .build();
        log.info("完成了minio的连接和配置！minio客户端对象为：{}",minioClient);
        return minioClient;
    }
}
