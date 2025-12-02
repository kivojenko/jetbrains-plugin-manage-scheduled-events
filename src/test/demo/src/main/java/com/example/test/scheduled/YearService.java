package com.example.test.scheduled;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


@Service
public class YearService {
    @Scheduled(cron = "0 0 12 * * ?")
    @SchedulerLock(name = "YearService_isItFutureLock")
    public void isItFuture() {
        System.out.println("Is it future?");
    }

}
