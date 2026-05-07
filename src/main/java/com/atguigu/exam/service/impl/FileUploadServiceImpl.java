package com.atguigu.exam.service.impl;

import com.atguigu.exam.config.properties.MinioProperties;
import com.atguigu.exam.service.FileUploadService;
import io.minio.*;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;


/**
 * projectName: com.atguigu.exam.service.impl
 *
 * @author: ziyi
 * description:
 */
@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {
    @Autowired
    MinioClient minio;
    @Autowired
    MinioProperties minioProperties;

    @Override
    public String uploadFile(String folder, MultipartFile file) throws Exception {
        // 1.先判断桶是否存在
        boolean bucketExists = minio.bucketExists(BucketExistsArgs.builder()
                .bucket(minioProperties.getBucketName()).build());
        // 2.不存在则创建
        if (!bucketExists) {
            minio.makeBucket(MakeBucketArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .build());
            String config = """
                    {
                        "Statement" : [ {
                            "Action" : "s3:GetObject",
                            "Effect" : "Allow",
                            "Principal" : "*",
                            "Resource" : "arn:aws:s3:::%s/*"
                        } ],
                        "Version" : "2012-10-17"
                    }
                    """.formatted(minioProperties.getBucketName());
            minio.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .config(config)
                    .build());
        }
        // 3.处理在minio服务器中的重名覆盖问题
        String name = folder + "/" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "/" +
                UUID.randomUUID().toString().replaceAll("-", "") + "_" + file.getOriginalFilename();
        log.debug("文件上传核心业务方法，处理后的文件对象名：{}", name);
        // 4.上传文件
        minio.putObject(PutObjectArgs.builder()
                .bucket(minioProperties.getBucketName())
                .contentType(file.getContentType())
                .object(name)
                .stream(file.getInputStream(), file.getSize(), -1)
                .build());
        String url = String.join("/", minioProperties.getEndpoint(), minioProperties.getBucketName(), name);
        log.info("文件上传核心业务，完成{}文件上传，返回地址为：{}", name, url);
        return url;
    }
}
