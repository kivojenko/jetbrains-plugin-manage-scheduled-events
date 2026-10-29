package com.example.test.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class CleanupJob implements Job {
    @Override
    public void execute(JobExecutionContext context) {
        System.out.println("Cleaning up");
    }
}
