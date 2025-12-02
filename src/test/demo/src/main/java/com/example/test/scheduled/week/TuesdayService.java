package com.example.test.scheduled.week;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TuesdayService {
    @Scheduled(cron = "*/15 * * * * *")
    @SchedulerLock(name = "TuesdayService_isItMondayLock")
    public void isItMonday() {
        System.out.println("Is it Monday?");
    }

    @Scheduled(cron = "0 0 2 */3 * *")
    @SchedulerLock(name = "TuesdayService_isItTuesdayLock")
    public void isItTuesday() {
        System.out.println("Is it Tuesday?");
    }


    @Scheduled(cron = "0 0 * * * 0")
    @SchedulerLock(name = "TuesdayService_isItWednesdayLock")
    public void isItWednesday() {
        System.out.println("Is it Wednesday?");
    }
}
