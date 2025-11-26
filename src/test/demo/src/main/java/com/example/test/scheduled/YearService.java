package com.example.test.scheduled;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class YearService {
    @Scheduled(cron = "0 0 12 * * ?")
    public void isItFuture() {
        System.out.println("Is it future?");
    }

}
