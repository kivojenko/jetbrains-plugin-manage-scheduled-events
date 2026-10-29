package com.example.test.quartz;

import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class InvoiceJob extends QuartzJobBean {
    @Override
    protected void executeInternal(JobExecutionContext context) {
        System.out.println("Sending invoices");
    }
}
