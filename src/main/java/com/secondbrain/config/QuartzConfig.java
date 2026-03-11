package com.secondbrain.config;

import org.quartz.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public org.quartz.JobDetail reviewJobDetail() {
        return org.quartz.JobBuilder.newJob(ReviewJob.class)
                .withIdentity("reviewJob", "group1")
                .storeDurably()
                .build();
    }

    @Bean
    public org.quartz.Trigger reviewTrigger() {
        return org.quartz.TriggerBuilder.newTrigger()
                .forJob(reviewJobDetail())
                .withIdentity("reviewTrigger", "group1")
                .withSchedule(org.quartz.CronScheduleBuilder.cronSchedule("0 0 8 * * ?"))
                .build();
    }
}
