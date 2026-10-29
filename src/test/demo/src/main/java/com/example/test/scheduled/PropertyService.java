package com.example.test.scheduled;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Crons read from application.yml through placeholders. */
@Service
public class PropertyService {
    /** Set in application.yml, overridden by the staging profile. */
    @Scheduled(cron = "${jobs.backup.cron}")
    public void backup() {
        System.out.println("Backing up");
    }

    /** Not in any config file, so the placeholder's default is used. */
    @Scheduled(cron = "${jobs.digest.cron:0 0 7 * * *}")
    public void digest() {
        System.out.println("Sending digest");
    }

    /** Not in any config file and no default: shown as unresolved. */
    @Scheduled(cron = "${jobs.missing.cron}")
    public void missing() {
        System.out.println("Never configured");
    }
}
