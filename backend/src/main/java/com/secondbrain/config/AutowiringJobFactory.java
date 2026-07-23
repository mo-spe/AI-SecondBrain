package com.secondbrain.config;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.stereotype.Component;

/** 支持构造器注入的 Quartz JobFactory. <p>继承 SpringBeanJobFactory 并通过 AutowireCapableBeanFactory 注入依赖</p> */
@Component
public class AutowiringJobFactory extends SpringBeanJobFactory implements ApplicationContextAware {

    private AutowireCapableBeanFactory beanFactory;

    /**
     * 设置应用上下文并获取 AutowireCapableBeanFactory.
     *
     * @param applicationContext 应用上下文
     * @return void
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
    }

    /**
     * 创建 Job 实例并通过构造器注入依赖.
     *
     * @param bundle 触发器触发时的上下文信息
     * @return 已注入依赖的 Job 实例
     * @throws Exception 创建实例异常
     */
    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
        return beanFactory.autowire(bundle.getJobDetail().getJobClass(),
                AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false);
    }
}
