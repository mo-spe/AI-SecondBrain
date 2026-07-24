package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.entity.AiModel;
import com.secondbrain.entity.AiProvider;
import com.secondbrain.entity.UserAiConfig;
import com.secondbrain.entity.UserAiProviderKey;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.AiModelMapper;
import com.secondbrain.mapper.AiProviderMapper;
import com.secondbrain.mapper.UserAiConfigMapper;
import com.secondbrain.mapper.UserAiProviderKeyMapper;
import com.secondbrain.service.AiProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI服务商管理服务实现.
 */
@Service
public class AiProviderServiceImpl implements AiProviderService {

    private static final Logger log = LoggerFactory.getLogger(AiProviderServiceImpl.class);

    private final AiProviderMapper aiProviderMapper;
    private final AiModelMapper aiModelMapper;
    private final UserAiConfigMapper userAiConfigMapper;
    private final UserAiProviderKeyMapper userAiProviderKeyMapper;

    public AiProviderServiceImpl(AiProviderMapper aiProviderMapper,
                                  AiModelMapper aiModelMapper,
                                  UserAiConfigMapper userAiConfigMapper,
                                  UserAiProviderKeyMapper userAiProviderKeyMapper) {
        this.aiProviderMapper = aiProviderMapper;
        this.aiModelMapper = aiModelMapper;
        this.userAiConfigMapper = userAiConfigMapper;
        this.userAiProviderKeyMapper = userAiProviderKeyMapper;
    }

    @Override
    public List<AiProvider> listProviders(boolean includeDisabled) {
        LambdaQueryWrapper<AiProvider> wrapper = new LambdaQueryWrapper<>();
        if (!includeDisabled) {
            wrapper.eq(AiProvider::getIsEnabled, 1);
        }
        wrapper.orderByAsc(AiProvider::getSortOrder);
        return aiProviderMapper.selectList(wrapper);
    }

    @Override
    public AiProvider getById(Long id) {
        AiProvider provider = aiProviderMapper.selectById(id);
        if (provider == null) {
            throw new BusinessException(404, "服务商不存在");
        }
        return provider;
    }

    @Override
    @Transactional
    public AiProvider save(AiProvider provider) {
        // 检查code唯一性
        LambdaQueryWrapper<AiProvider> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProvider::getCode, provider.getCode());
        if (provider.getId() != null) {
            wrapper.ne(AiProvider::getId, provider.getId());
        }
        if (aiProviderMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "服务商标识 " + provider.getCode() + " 已存在");
        }

        if (provider.getId() != null) {
            aiProviderMapper.updateById(provider);
            log.info("updated_ai_provider id={} code={}", provider.getId(), provider.getCode());
        } else {
            aiProviderMapper.insert(provider);
            log.info("created_ai_provider id={} code={}", provider.getId(), provider.getCode());
        }
        return provider;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AiProvider provider = getById(id);

        long userCount = countUsersByProviderId(id);
        if (userCount > 0) {
            throw new BusinessException(400,
                    "该服务商被 " + userCount + " 个用户使用，无法删除");
        }

        // 删除关联模型
        LambdaQueryWrapper<AiModel> modelWrapper = new LambdaQueryWrapper<>();
        modelWrapper.eq(AiModel::getProviderId, id);
        aiModelMapper.delete(modelWrapper);

        aiProviderMapper.deleteById(id);
        log.info("deleted_ai_provider id={} code={}", id, provider.getCode());
    }

    @Override
    public List<AiModel> listModels(Long providerId, boolean includeDisabled) {
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModel::getProviderId, providerId);
        if (!includeDisabled) {
            wrapper.eq(AiModel::getIsEnabled, 1);
        }
        wrapper.orderByAsc(AiModel::getSortOrder);
        return aiModelMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public AiModel saveModel(AiModel model) {
        // 检查同一服务商下model_name唯一性
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModel::getProviderId, model.getProviderId());
        wrapper.eq(AiModel::getModelName, model.getModelName());
        if (model.getId() != null) {
            wrapper.ne(AiModel::getId, model.getId());
        }
        if (aiModelMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "该服务商下模型名称 " + model.getModelName() + " 已存在");
        }

        if (model.getId() != null) {
            aiModelMapper.updateById(model);
            log.info("updated_ai_model id={} name={}", model.getId(), model.getModelName());
        } else {
            aiModelMapper.insert(model);
            log.info("created_ai_model id={} name={}", model.getId(), model.getModelName());
        }
        return model;
    }

    @Override
    @Transactional
    public void deleteModel(Long id) {
        aiModelMapper.deleteById(id);
        log.info("deleted_ai_model id={}", id);
    }

    @Override
    public long countUsersByProviderId(Long providerId) {
        long configCount = userAiConfigMapper.selectCount(
                new LambdaQueryWrapper<UserAiConfig>().eq(UserAiConfig::getProviderId, providerId));
        long keyCount = userAiProviderKeyMapper.selectCount(
                new LambdaQueryWrapper<UserAiProviderKey>().eq(UserAiProviderKey::getProviderId, providerId));
        return configCount + keyCount;
    }
}
