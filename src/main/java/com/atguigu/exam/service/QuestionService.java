package com.atguigu.exam.service;

import com.atguigu.exam.entity.Question;
import com.atguigu.exam.vo.QuestionImportVo;
import com.atguigu.exam.vo.QuestionPageVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 题目业务服务接口 - 定义题目相关的业务逻辑
 * 
 * Spring Boot三层架构教学要点：
 * 1. Service层：业务逻辑层，位于Controller和Mapper之间
 * 2. 接口设计：定义业务方法规范，便于不同实现类的切换
 * 3. 继承IService：使用MyBatis Plus提供的通用服务接口，减少重复代码
 * 4. 事务管理：Service层是事务的边界，复杂业务操作应该加@Transactional
 * 5. 业务封装：将复杂的数据操作封装成有业务意义的方法
 * 
 * MyBatis Plus教学：
 * - IService<T>：提供基础的CRUD方法（save、update、remove、list等）
 * - 自定义方法：在接口中定义特定业务需求的方法
 * - 实现类：继承ServiceImpl<Mapper, Entity>并实现自定义业务方法
 * 
 * 设计原则：
 * - 单一职责：专门处理题目相关的业务逻辑
 * - 开闭原则：通过接口定义，便于扩展新的实现
 * - 依赖倒置：Controller依赖接口而不是具体实现
 * 
 * @author 智能学习平台开发团队
 * @version 1.0
 */
public interface QuestionService extends IService<Question> {


    void customPageService(IPage<Question> customPage, QuestionPageVo questionPageVo);

    /**
     *用java代码查询所有题目信息
     *
     * @param customPage
     * @param questionPageVo
     */
    void customPageServiceForJava(IPage<Question> customPage, QuestionPageVo questionPageVo);

    /**
     *
     *根据id查询题目信息
     * @param id
     * @return {@link Question }
     */
    Question customDetailQuestion(Long id);

    /**
     *保存题目信息
     *
     * @param question
     */
    void customSaveQuestion(Question question);

    /**
     *更新题目信息
     *
     * @param question
     */
    void customUpdateQuestion(Question question);

    /**
     *根据id删除题目
     *
     * @param id
     */
    void customRemoveQuestionById(Long id);

    /**
     *
     *展示热门题目
     * @param size
     * @return {@link List }<{@link Question }>
     */
    List<Question> customPopularQuestion(Integer size);

    /**
     *预览题目
     *
     * @param file
     * @return {@link List }<{@link QuestionImportVo }>
     * @throws IOException
     */
    List<QuestionImportVo> previewExcel(MultipartFile file) throws IOException;

    /**
     *导入解析后的题目
     *
     * @param questions
     * @return int
     */
    int batchSaveExcelQuestion(List<QuestionImportVo> questions);
}
