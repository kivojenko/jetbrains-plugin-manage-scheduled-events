package com.kivojenko.plugin.manage_scheduled_events.cron;

import junit.framework.TestCase;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.kivojenko.plugin.manage_scheduled_events.cron.CronExpression.Flavor.QUARTZ;
import static com.kivojenko.plugin.manage_scheduled_events.cron.CronExpression.Flavor.SPRING;

public class CronExpressionTest extends TestCase {
    private static final ZoneId UTC = ZoneId.of("UTC");
    /** A Wednesday. */
    private static final ZonedDateTime START = ZonedDateTime.of(2026, 10, 28, 10, 7, 30, 0, UTC);

    public void testEveryFifteenSeconds() {
        assertEquals(List.of("10:07:45", "10:08:00", "10:08:15"), times("*/15 * * * * *", SPRING, 3));
        assertEquals("Every 15 seconds", describe("*/15 * * * * *", SPRING));
    }

    public void testWorkingHours() {
        var cron = "0 0/15 9-17 ? * MON-FRI";
        assertEquals(List.of("2026-10-28T10:15", "2026-10-28T10:30"), dateTimes(cron, QUARTZ, 2));
        assertEquals("Every 15 minutes, between 09:00 and 17:59, on Monday through Friday", describe(cron, QUARTZ));
    }

    public void testDailyAt() {
        assertEquals(List.of("2026-10-28T12:00", "2026-10-29T12:00"), dateTimes("0 0 12 * * ?", QUARTZ, 2));
        assertEquals("At 12:00, every day", describe("0 0 12 * * ?", QUARTZ));
        assertEquals("At 00:00, every day", describe("@daily", SPRING));
    }

    public void testSundayIsZeroAndSevenInSpringButOneInQuartz() {
        var spring = dateTimes("0 0 4 * * 0", SPRING, 1);
        assertEquals(spring, dateTimes("0 0 4 * * 7", SPRING, 1));
        assertEquals(spring, dateTimes("0 0 4 * * SUN", SPRING, 1));
        assertEquals(spring, dateTimes("0 0 4 ? * 1", QUARTZ, 1));
        assertEquals(List.of("2026-11-01T04:00"), spring);
        assertEquals("At 04:00, on Sunday", describe("0 0 4 ? * SUN", QUARTZ));
    }

    public void testLastDayOfMonthWithYears() {
        var cron = "0 0 8 L * ? 2026-2030";
        assertEquals(List.of("2026-10-31T08:00", "2026-11-30T08:00", "2026-12-31T08:00"), dateTimes(cron, QUARTZ, 3));
        assertEquals("At 08:00, on the last day of the month, in 2026 through 2030", describe(cron, QUARTZ));
        assertTrue(CronExpression.parse("0 0 8 L * ? 2020", QUARTZ).nextExecution(START).isEmpty());
    }

    public void testWeekdaysOfMonth() {
        assertEquals(List.of("2026-11-09T00:00"), dateTimes("0 0 0 ? * MON#2", QUARTZ, 1));
        assertEquals(List.of("2026-10-30T00:00"), dateTimes("0 0 0 ? * 6L", QUARTZ, 1));
        // 31 October 2026 is a Saturday
        assertEquals(List.of("2026-10-30T00:00"), dateTimes("0 0 0 LW * ?", QUARTZ, 1));
        assertEquals("At 00:00, on the second Monday of the month", describe("0 0 0 ? * MON#2", QUARTZ));
    }

    public void testHourly() {
        assertEquals(List.of("2026-10-28T11:00"), dateTimes("0 0 * * * *", SPRING, 1));
        assertEquals("Every hour", describe("0 0 * * * *", SPRING));
        assertEquals("Every hour, on Sunday", describe("0 0 * * * 0", SPRING));
        assertEquals("At 02:00, every 3 days", describe("0 0 2 */3 * *", SPRING));
    }

    public void testNeverRuns() {
        assertTrue(CronExpression.parse("0 0 0 30 2 *", SPRING).nextExecution(START).isEmpty());
    }

    public void testInvalid() {
        assertInvalid("0 0 0 0 * *", SPRING, "Invalid day 0: must be between 1 and 31");
        assertInvalid("0 61 * * * *", SPRING, "Invalid minute 61: must be between 0 and 59");
        assertInvalid("0 0 * * *", SPRING, "Expected 6 fields but found 5");
        assertInvalid("0 0 0 * * ? 2026", SPRING, "Expected 6 fields but found 7");
        assertInvalid("0 0 0 * * *", QUARTZ, "Either day of month or day of week must be '?', but not both");
        assertInvalid("0 0 0 ? * ?", QUARTZ, "Either day of month or day of week must be '?', but not both");
        assertInvalid("0 0 0 * FOO *", SPRING, "Invalid month 'FOO'");
        assertInvalid("0 0 ? * * *", SPRING, "'?' is only allowed for day of month and day of week");
        assertInvalid("0 */0 * * * *", SPRING, "Step in '*/0' must be positive");
    }

    private static void assertInvalid(String cron, CronExpression.Flavor flavor, String message) {
        try {
            CronExpression.parse(cron, flavor);
            fail("Expected " + cron + " to be invalid");
        } catch (IllegalArgumentException e) {
            assertEquals(message, e.getMessage());
        }
    }

    private static String describe(String cron, CronExpression.Flavor flavor) {
        return CronExpression.parse(cron, flavor).describe();
    }

    private static List<String> times(String cron, CronExpression.Flavor flavor, int count) {
        return runs(cron, flavor, count).stream().map(t -> t.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME)).toList();
    }

    private static List<String> dateTimes(String cron, CronExpression.Flavor flavor, int count) {
        return runs(cron, flavor, count).stream().map(t -> t.toLocalDateTime().toString()).toList();
    }

    private static List<ZonedDateTime> runs(String cron, CronExpression.Flavor flavor, int count) {
        var expression = CronExpression.parse(cron, flavor);
        List<ZonedDateTime> runs = new ArrayList<>();
        var last = START;
        for (int i = 0; i < count; i++) {
            last = expression.nextExecution(last).orElseThrow();
            runs.add(last);
        }
        return runs;
    }
}
