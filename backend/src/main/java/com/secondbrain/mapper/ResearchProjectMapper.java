package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.ResearchProject;
import org.apache.ibatis.annotations.Mapper;

/**
 * 研究项目 Mapper.
 *
 * <p>继承 MyBatis-Plus BaseMapper，自动获得标准 CRUD 方法。</p>
 *
 * @author AI
 */
@Mapper
public interface ResearchProjectMapper extends BaseMapper<ResearchProject> {
}
