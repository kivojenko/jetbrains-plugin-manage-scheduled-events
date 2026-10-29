package com.example.test.quartz;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Quartz crons read from application.yml. */
@Configuration
public class PropertyQuartzConfig {

    /** Through @ConfigurationProperties getters: jobs.report.cron, overridden by the prod profile. */
    @Bean
    public Trigger propertyReportTrigger(JobProperties jobs) {
        var job = JobBuilder.newJob(ReportJob.class).withIdentity("propertyReport").build();
        return TriggerBuilder.newTrigger()
                .forJob(job)
                .withSchedule(CronScheduleBuilder.cronSchedule(jobs.getReport().getCron()))
                .build();
    }

    /** Through @Value on a parameter. */
    @Bean
    public Trigger archiveTrigger(@Value("${jobs.archive-cron}") String archiveCron) {
        var job = JobBuilder.newJob(ArchiveJob.class).withIdentity("archive").build();
        return TriggerBuilder.newTrigger()
                .forJob(job)
                .withSchedule(CronScheduleBuilder.cronSchedule(archiveCron))
                .build();
    }
}
