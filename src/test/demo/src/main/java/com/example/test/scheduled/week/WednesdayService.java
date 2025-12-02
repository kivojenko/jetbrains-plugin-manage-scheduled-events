package com.example.test.scheduled.week;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WednesdayService {
    @Scheduled(cron = "0 0 0 0 * *")
    @SchedulerLock(name = "WednesdayService_isItMondayLock")
    public void isItMonday() {
        System.out.println("Is it Monday?");
    }
}
