package com.atguigu.exam.config.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * projectName: day23_exam-system-server
 *
 * @author: 赵伟风
 * description: 读取Minio相关的参数
 */
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioProperties {

//    端点 minio.endpoint  账号 minio.username 密码 minio.password  桶名 mimio.bucket-name
    private String endpoint;
    private String username;
    private String password;
    // @Value() //依赖注入！ 非引用类型！
    private String bucketName;

    /*
         yaml              java         数据库
       bucket-name       bucketName    bucket_name
     */
}
