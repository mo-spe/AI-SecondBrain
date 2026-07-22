package com.secondbrain.config;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.stereotype.Component;

/**
 * 支持构造器注入的 Quartz JobFactory。
 * 继承 SpringBeanJobFactory 并覆盖实例化逻辑，使 Quartz 能够通过 Spring 构造器注入创建 Job 实例。
 */
@Component
public class AutowiringJobFactory extends SpringBeanJobFactory implements ApplicationContextAware {

    private AutowireCapableBeanFactory beanFactory;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
    }

    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
        return beanFactory.autowire(bundle.getJobDetail().getJobClass(),
                AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false);
    }
}
