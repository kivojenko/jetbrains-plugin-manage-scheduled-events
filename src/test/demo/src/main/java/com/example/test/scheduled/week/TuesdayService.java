package com.example.test.scheduled.week;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TuesdayService {
    @Scheduled(cron = "*/15 * * * * *")
    public void isItMonday() {
        System.out.println("Is it Monday?");
    }

    @Scheduled(cron = "0 0 2 */3 * *")
    public void isItTuesday() {
        System.out.println("Is it Tuesday?");
    }


    @Scheduled(cron = "0 0 * * * 0")
    public void isItWednesday() {
        System.out.println("Is it Wednesday?");
    }
}
