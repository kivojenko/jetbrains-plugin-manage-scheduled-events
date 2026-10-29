package com.kivojenko.plugin.manage_scheduled_events.ui.panel;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public final class CronUtils {
    private static final CronParser parser =
            new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING));

    private CronUtils() {
    }

    public static List<ZonedDateTime> nextDay(String cron) {
        return executionsWithinDays(cron, 1);
    }

    public static List<ZonedDateTime> nextWeek(String cron) {
        return executionsWithinDays(cron, 7);
    }

    private static List<ZonedDateTime> executionsWithinDays(String cron, int days) {
        var execution = ExecutionTime.forCron(parser.parse(cron));
        var now = ZonedDateTime.now();
        var end = now.plusDays(days);
        List<ZonedDateTime> times = new ArrayList<>();

        for (var next = execution.nextExecution(now); next.isPresent() && next.get().isBefore(end);
             next = execution.nextExecution(next.get())) {
            times.add(next.get());
        }
        return times;
    }
}
