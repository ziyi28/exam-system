package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.Banner;
import com.atguigu.exam.mapper.BannerMapper;
import com.atguigu.exam.service.BannerService;

import com.atguigu.exam.service.FileUploadService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.management.RuntimeMBeanException;
import javax.xml.crypto.Data;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 轮播图服务实现类
 */
@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {

    @Autowired
    private FileUploadService fileUploadService;

    @Override
    public String uploadBannerImage(MultipartFile file) throws Exception {
        //1.非空校验
        if (file == null || file.isEmpty()){
            throw new RuntimeException("上传轮播图图片失败！原因：上传的文件为空！");
        }
        //2.格式校验
        if (!file.getContentType().startsWith("image")){
            throw new RuntimeException("上传轮播图图片失败！原因：上传的文件类型错误,非图片！");
        }
        //3.调用核心业务
        String url = fileUploadService.uploadFile("banners", file);
        //4.返回地址
        return url;
    }


    @Override
    public void addBanner(Banner banner) throws Exception {
        //1.检查默认值 isActive = true  sortOrder = 5
        if (banner.getIsActive() == null){
            banner.setIsActive(true);
        }
        if (banner.getSortOrder() == null){
            banner.setSortOrder(5);
        }
        //2. createTime updateTime isDeleted 前端一定不传值！
        /*
            类似时间和逻辑删除字段！所有的表中都有！
            所有业务的保存和更新都涉及到了值的填写！
            而且他们填写的业务还是一样！冗余 重复 烦人！！
            思路1： 数据库维度 （表中已经处理了）
               mysql -> 默认值 -> is_deleted default 0 ;  【推荐】
                        时间自动填充 -> 列名 类型 约束 （可以时间自动填充）  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP 【不推荐】
            思路2： mybatis-plus维度 【时间类，推荐使用mybatis-plus】
                   底层核心就是手动赋值！！
                   https://baomidou.com/guides/auto-fill-field/

         */
//        banner.setCreateTime(new Date());
//        banner.setUpdateTime(new Date());
//        banner.setIsDeleted((byte) 0);
        //3.继续保存banner
        boolean saved = save(banner);
        if (!saved){
            throw new Exception("轮播图数据保存失败！！");
        }
    }

    @Override
    public void updateBanner(Banner banner) throws Exception {
        //1.检查默认值 isActive = true  sortOrder = 5
        if (banner.getIsActive() == null){
            banner.setIsActive(true);
        }
        if (banner.getSortOrder() == null){
            banner.setSortOrder(5);
        }

        //2.调用修改即可
        boolean updated = updateById(banner);
        if (!updated){
            throw new Exception("轮播图修改失败！");
        }
    }

}