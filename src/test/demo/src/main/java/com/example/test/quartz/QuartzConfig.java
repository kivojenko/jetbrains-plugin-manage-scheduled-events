package com.example.test.quartz;

import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;

/** Each trigger is matched to its job in a different way, to exercise the plugin's Quartz lookup. */
@Configuration
public class QuartzConfig {
    private static final String NIGHTLY = "0 30 2 * * ?";

    @Bean
    public JobDetail reportJobDetail() {
        return JobBuilder.newJob(ReportJob.class).withIdentity("report").storeDurably().build();
    }

    /** Job injected by parameter name. */
    @Bean
    public Trigger reportTrigger(JobDetail reportJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(reportJobDetail)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0/15 9-17 ? * MON-FRI"))
                .build();
    }

    /** Job class in the same method; cron from a constant. */
    @Bean
    public Trigger cleanupTrigger() {
        var job = JobBuilder.newJob(CleanupJob.class).withIdentity("cleanup").build();
        return TriggerBuilder.newTrigger()
                .forJob(job)
                .withSchedule(CronScheduleBuilder.cronSchedule(NIGHTLY))
                .build();
    }

    @Bean
    public JobDetailFactoryBean invoiceJobDetail() {
        var factory = new JobDetailFactoryBean();
        factory.setJobClass(InvoiceJob.class);
        factory.setDurability(true);
        return factory;
    }

    /** Job found through a called bean method; Quartz-only cron with a year field. */
    @Bean
    public CronTriggerFactoryBean invoiceTrigger() {
        var factory = new CronTriggerFactoryBean();
        factory.setJobDetail(invoiceJobDetail().getObject());
        factory.setCronExpression("0 0 8 L * ? 2026-2030");
        return factory;
    }
}
