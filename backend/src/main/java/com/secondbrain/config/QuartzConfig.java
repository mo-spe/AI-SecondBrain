package com.secondbrain.config;

import org.quartz.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Quartz调度配置. <p>注册复习任务 JobDetail 与 Trigger</p> */
@Configuration
public class QuartzConfig {

    /**
     * 创建复习任务 JobDetail.
     *
     * @return 复习任务 JobDetail
     */
    @Bean
    public org.quartz.JobDetail reviewJobDetail() {
        return org.quartz.JobBuilder.newJob(ReviewJob.class)
                .withIdentity("reviewJob", "group1")
                .storeDurably()
                .build();
    }

    /**
     * 创建复习任务 Trigger.
     *
     * @return 复习任务 Trigger
     */
    @Bean
    public org.quartz.Trigger reviewTrigger() {
        return org.quartz.TriggerBuilder.newTrigger()
                .forJob(reviewJobDetail())
                .withIdentity("reviewTrigger", "group1")
                .withSchedule(org.quartz.CronScheduleBuilder.cronSchedule("0 0 8 * * ?"))
                .build();
    }
}
