package com.atguigu.exam.controller;


import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.Banner;
import com.atguigu.exam.service.BannerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 轮播图控制器 - 处理轮播图管理相关的HTTP请求
 * 包括图片上传、轮播图的CRUD操作、状态切换等功能
 */
@RestController  // REST控制器，返回JSON数据
@RequestMapping("/api/banners")  // 轮播图API路径前缀
@CrossOrigin  // 允许跨域访问
@Tag(name = "轮播图管理", description = "轮播图相关操作，包括图片上传、轮播图增删改查、状态管理等功能")  // Swagger API分组
@Slf4j
public class BannerController {


    @Autowired
    private BannerService bannerService;

    /**
     * 上传轮播图图片
     * @param file 图片文件
     * @return 图片访问URL
     */
    @PostMapping("/upload-image")  // 处理POST请求
    @Operation(summary = "上传轮播图图片", description = "将图片文件上传到MinIO服务器，返回可访问的图片URL")  // API描述
    public Result<String> uploadBannerImage(
            @Parameter(description = "要上传的图片文件，支持jpg、png、gif等格式，大小限制5MB") 
            @RequestParam("file") MultipartFile file) throws Exception {
        //调用service上传业务！返回回显地址！
        String url = bannerService.uploadBannerImage(file);
        log.info("上传轮播图图片成功！ 回显地址为：{}",url);
        return Result.success(url, "图片上传成功");
    }
    
    /**
     * 获取启用的轮播图（前台首页使用）
     * @return 轮播图列表
     */
    @GetMapping("/active")  // 处理GET请求
    @Operation(summary = "获取启用的轮播图", description = "获取状态为启用的轮播图列表，供前台首页展示使用")  // API描述
    public Result<List<Banner>> getActiveBanners() {
        //添加查询条件
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Banner::getIsActive,1).orderByAsc(Banner::getSortOrder);
        //查询
        List<Banner> list = bannerService.list(wrapper);
        //打印日志
        log.info("查询已经启用的轮播图的集合：{}",list);
        return Result.success(list,"查询激活状态banner信息成功！");
    }
    
    /**
     * 获取所有轮播图（管理后台使用）
     * @return 轮播图列表
     */
    @GetMapping("/list")  // 处理GET请求
    @Operation(summary = "获取所有轮播图", description = "获取所有轮播图列表，包括启用和禁用的，供管理后台使用")  // API描述
    public Result<List<Banner>> getAllBanners() {
        //查询轮播图并排序
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Banner::getSortOrder);
        //调用service方法查询
        List<Banner> list = bannerService.list(wrapper);
        //封装结果,打印日志
        log.info("根据sortOrder排序后查出的banner集合：{}",list);

        return Result.success(list,"查询所有banner信息成功！");
    }
    
    /**
     * 根据ID获取轮播图
     * @param id 轮播图ID
     * @return 轮播图详情
     */
    @GetMapping("/{id}")  // 处理GET请求
    @Operation(summary = "根据ID获取轮播图", description = "根据轮播图ID获取单个轮播图的详细信息")  // API描述  
    public Result<Banner> getBannerById(@Parameter(description = "轮播图ID") @PathVariable Long id) {
        //根据id查询即可
        Banner banner = bannerService.getById(id);
        //进行结果拼接
        if (banner != null) {
            log.info("轮播图详情查询成功！查询结果为：{}",banner);
            return Result.success(banner,"轮播图详情查询成功！");
        }
        log.info("轮播图详情查询失败！没有查询指定到id:{}的轮播图信息！",id);
        return Result.error("轮播图详情查询失败！");
    }
    
    /**
     * 添加轮播图
     * @param banner 轮播图对象
     * @return 操作结果
     */
    @PostMapping("/add")  // 处理POST请求
    @Operation(summary = "添加轮播图", description = "创建新的轮播图，需要提供图片URL、标题、跳转链接等信息")  // API描述
    public Result<String> addBanner(@RequestBody Banner banner) throws Exception {
        bannerService.addBanner(banner);
        log.info("轮播图保存数据业务调用成功！");
        return Result.success("轮播图数据保存成功！");
    }
    
    /**
     * 更新轮播图
     * @param banner 轮播图对象
     * @return 操作结果
     */
    @PutMapping("/update")  // 处理PUT请求
    @Operation(summary = "更新轮播图", description = "更新轮播图的信息，包括图片、标题、跳转链接、排序等")  // API描述
    public Result<String> updateBanner(@RequestBody Banner banner) throws Exception {
        bannerService.updateBanner(banner);
        log.info("轮播图数据修改业务成功！！");
        return Result.success("轮播图修改成功");
    }
    
    /**
     * 删除轮播图
     * @param id 轮播图ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{id}")  // 处理DELETE请求
    @Operation(summary = "删除轮播图", description = "根据ID删除指定的轮播图")  // API描述
    public Result<String> deleteBanner(@Parameter(description = "轮播图ID") @PathVariable Long id) {
        //直接删除
        boolean b = bannerService.removeById(id);
        if (b) {
            log.info("轮播图id为{}的轮播图删除成功",id);
            return Result.success("轮播图删除成功");

        }
        log.info("轮播图id为{}的轮播图删除失败",id);
        return Result.error("轮播图数据删除失败！");
    }
    
    /**
     * 启用/禁用轮播图
     * @param id 轮播图ID
     * @param isActive 是否启用
     * @return 操作结果
     */
    @PutMapping("/toggle/{id}")  // 处理PUT请求
    @Operation(summary = "切换轮播图状态", description = "启用或禁用指定的轮播图，禁用后不会在前台显示")  // API描述
    public Result<String> toggleBannerStatus(
            @Parameter(description = "轮播图ID") @PathVariable Long id, 
            @Parameter(description = "是否启用，true为启用，false为禁用") @RequestParam Boolean isActive) {
        //创建updateWrapper
        LambdaUpdateWrapper<Banner> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        //设置修改的条件 id = x
        lambdaUpdateWrapper.eq(Banner::getId,id);
        lambdaUpdateWrapper.set(Banner::getIsActive,isActive);
        //更改
        boolean update = bannerService.update(lambdaUpdateWrapper);
        if (update){
            log.info("轮播图状态修改成功，修改后的状态为:",isActive);
            return  Result.success("轮播图状态更改成功");
        }


        log.info("轮播图状态修改失败！原计划的目标状态为：{}",isActive);
        return Result.error("轮播图的激活状态修改失败！");
    }
} 