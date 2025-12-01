package com.example.test.scheduled.week;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MondayService {
    @Scheduled(cron = "0 0 0 * * *")
    public void isItMonday() {
        System.out.println("Is it Monday?");
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void itIsNotMonday() {
        System.out.println("It is not Monday");
    }
}
