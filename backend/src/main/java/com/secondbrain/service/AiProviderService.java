package com.secondbrain.service;

import com.secondbrain.entity.AiModel;
import com.secondbrain.entity.AiProvider;

import java.util.List;

/**
 * AI服务商管理服务.
 * <p>提供服务商和预设模型的维护能力，供管理端和用户端使用</p>
 */
public interface AiProviderService {

    /**
     * 查询服务商列表.
     *
     * @param includeDisabled 是否包含已禁用的服务商
     * @return 服务商列表
     */
    List<AiProvider> listProviders(boolean includeDisabled);

    /**
     * 根据ID查询服务商.
     *
     * @param id 服务商ID
     * @return 服务商信息
     */
    AiProvider getById(Long id);

    /**
     * 新增或更新服务商.
     *
     * @param provider 服务商信息
     * @return 保存后的服务商
     */
    AiProvider save(AiProvider provider);

    /**
     * 删除服务商.
     *
     * @param id 服务商ID
     */
    void delete(Long id);

    /**
     * 查询某服务商下的模型列表.
     *
     * @param providerId     服务商ID
     * @param includeDisabled 是否包含已禁用的模型
     * @return 模型列表
     */
    List<AiModel> listModels(Long providerId, boolean includeDisabled);

    /**
     * 新增模型.
     *
     * @param model 模型信息
     * @return 保存后的模型
     */
    AiModel saveModel(AiModel model);

    /**
     * 删除模型.
     *
     * @param id 模型ID
     */
    void deleteModel(Long id);

    /**
     * 统计使用某服务商的用户数.
     *
     * @param providerId 服务商ID
     * @return 用户数
     */
    long countUsersByProviderId(Long providerId);
}
