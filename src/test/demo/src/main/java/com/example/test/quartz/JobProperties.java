package com.example.test.quartz;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jobs")
public class JobProperties {
    private final Report report = new Report();
    private String archiveCron;

    public Report getReport() {
        return report;
    }

    public String getArchiveCron() {
        return archiveCron;
    }

    public void setArchiveCron(String archiveCron) {
        this.archiveCron = archiveCron;
    }

    public static class Report {
        private String cron;

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }
}
