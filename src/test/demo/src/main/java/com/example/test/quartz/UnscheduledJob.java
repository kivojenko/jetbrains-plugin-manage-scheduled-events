package com.example.test.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

/** Has no trigger, so the plugin lists it without a cron. */
public class UnscheduledJob implements Job {
    @Override
    public void execute(JobExecutionContext context) {
        System.out.println("Never scheduled");
    }
}
