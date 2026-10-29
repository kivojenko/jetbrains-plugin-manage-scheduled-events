package com.example.test.quartz;

import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class ReportJob extends QuartzJobBean {
    @Override
    protected void executeInternal(JobExecutionContext context) {
        System.out.println("Building report");
    }
}
