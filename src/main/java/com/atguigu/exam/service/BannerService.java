package com.atguigu.exam.service;

import com.atguigu.exam.entity.Banner;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;

/**
 * 轮播图服务接口
 */
public interface BannerService extends IService<Banner> {

    /**
     * 上传轮播图业务！ 做校验 + 调用核心上传业务方法
     * @param file 上传的文件
     * @return 回显的地址！ 如果失败！ 直接抛出异常！
     */
    String uploadBannerImage(MultipartFile file) throws Exception;

    /**
     * 保存轮播图信息
     * @param banner 轮播图信息对象
     */
    void addBanner(Banner banner) throws Exception;

    /**
     * 修改轮播图信息
     * @param banner 轮播图信息对象
     */
    void updateBanner(Banner banner) throws Exception;
}