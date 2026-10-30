package com.kivojenko.plugin.manage_scheduled_events.cron;

import org.jetbrains.annotations.Nullable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;

/**
 * A parsed Spring or Quartz cron: validates it, finds the next runs and describes it in English.
 * Fields: second, minute, hour, day of month, month, day of week and, for Quartz only, an optional year.
 */
public final class CronExpression {
    /** Spring and Quartz crons differ, e.g. in day-of-week numbering and the year field. */
    public enum Flavor {
        /** Spring 5.3+: day of week 0-7 with Sunday as 0 or 7; macros such as {@code @daily}. */
        SPRING,
        /** Quartz: day of week 1-7 with Sunday as 1; optional year; one of the day fields must be {@code ?}. */
        QUARTZ
    }

    /** How far ahead to look for a run before deciding a cron never runs again, e.g. {@code 0 0 0 30 2 *}. */
    private static final int MAX_YEARS_AHEAD = 100;
    private static final int MIN_YEAR = 1970;
    private static final int MAX_YEAR = 2099;

    private static final Map<String, String> SPRING_MACROS = Map.of(
            "@yearly", "0 0 0 1 1 *",
            "@annually", "0 0 0 1 1 *",
            "@monthly", "0 0 0 1 * *",
            "@weekly", "0 0 0 * * 0",
            "@daily", "0 0 0 * * *",
            "@midnight", "0 0 0 * * *",
            "@hourly", "0 0 * * * *");

    private static final String[] MONTHS = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
    private static final String[] DAYS = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
    private static final String[] ORDINALS = {"first", "second", "third", "fourth", "fifth"};

    private enum Kind {
        SECOND("second", 0, 59), MINUTE("minute", 0, 59), HOUR("hour", 0, 23), DAY_OF_MONTH("day", 1, 31),
        MONTH("month", 1, 12), DAY_OF_WEEK("day of the week", 0, 7), YEAR("year", MIN_YEAR, MAX_YEAR);

        final String unit;
        final int min;
        final int max;

        Kind(String unit, int min, int max) {
            this.unit = unit;
            this.min = min;
            this.max = max;
        }
    }

    /** One comma-separated part of a field. Days of the week are {@link DayOfWeek} values: Monday 1 to Sunday 7. */
    private sealed interface Item {
    }

    /** {@code *}, {@code 5}, {@code 1-5}, {@code *}{@code /15}, {@code 10/5} or {@code 1-30/2}; values as written. */
    private record Values(boolean all, int from, int to, int step) implements Item {
    }

    /** {@code L} or {@code L-3}: the last day of the month, or that many days before it. */
    private record LastDayOfMonth(int offset) implements Item {
    }

    /** {@code LW}. */
    private record LastWeekdayOfMonth() implements Item {
    }

    /** {@code 15W}: the weekday nearest the 15th, within the same month. */
    private record NearestWeekday(int day) implements Item {
    }

    /** {@code 5L} / {@code FRIL}: the last Friday of the month. */
    private record LastDayOfWeek(DayOfWeek day) implements Item {
    }

    /** {@code MON#2}: the second Monday of the month. */
    private record NthDayOfWeek(DayOfWeek day, int n) implements Item {
    }

    /** A parsed field; {@code values} holds what {@link Values} items match, days of the week as 1-7. */
    private record Field(Kind kind, boolean any, List<Item> items, BitSet values) {
        boolean isSingleValue() {
            return items.size() == 1 && items.getFirst() instanceof Values(
                    boolean all, int from, int v1, int step
            ) && !all && step == 1 && from == v1;
        }

        int single() {
            return ((Values) items.getFirst()).from;
        }

        /** Only plain values, e.g. {@code 9,13,17}. */
        boolean isValueList() {
            return items.stream().allMatch(i -> i instanceof Values(
                    boolean all, int from, int v1, int step
            ) && !all && step == 1 && from == v1);
        }
    }

    private final Flavor flavor;
    private final Field seconds;
    private final Field minutes;
    private final Field hours;
    private final Field daysOfMonth;
    private final Field months;
    private final Field daysOfWeek;
    private final @Nullable Field years;

    private CronExpression(Flavor flavor, Field[] fields) {
        this.flavor = flavor;
        seconds = fields[0];
        minutes = fields[1];
        hours = fields[2];
        daysOfMonth = fields[3];
        months = fields[4];
        daysOfWeek = fields[5];
        years = fields.length > 6 ? fields[6] : null;
    }

    /** @throws IllegalArgumentException with a message fit to show the user if the cron is invalid */
    public static CronExpression parse(String cron, Flavor flavor) {
        var text = cron.strip();
        if (flavor == Flavor.SPRING && text.startsWith("@")) {
            var expanded = SPRING_MACROS.get(text.toLowerCase(Locale.ROOT));
            if (expanded == null) throw new IllegalArgumentException("Unknown macro " + text);
            text = expanded;
        }
        if (text.isEmpty()) throw new IllegalArgumentException("Cron is empty");

        var parts = text.split("\\s+");
        var expected = flavor == Flavor.SPRING ? "6" : "6 or 7";
        if (parts.length < 6 || parts.length > (flavor == Flavor.SPRING ? 6 : 7)) {
            throw new IllegalArgumentException("Expected " + expected + " fields but found " + parts.length);
        }

        var kinds = Kind.values();
        var fields = new Field[parts.length];
        for (int i = 0; i < parts.length; i++) fields[i] = parseField(parts[i], kinds[i], flavor);

        if (flavor == Flavor.QUARTZ && "?".equals(parts[3]) == "?".equals(parts[5])) {
            throw new IllegalArgumentException("Either day of month or day of week must be '?', but not both");
        }
        return new CronExpression(flavor, fields);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Parsing

    private static Field parseField(String text, Kind kind, Flavor flavor) {
        var upper = text.toUpperCase(Locale.ROOT);
        if (upper.equals("?")) {
            if (kind != Kind.DAY_OF_MONTH && kind != Kind.DAY_OF_WEEK) {
                throw new IllegalArgumentException("'?' is only allowed for day of month and day of week");
            }
            return allValues(kind, flavor);
        }

        List<Item> items = new ArrayList<>();
        var values = new BitSet();
        for (var part : upper.split(",", -1)) {
            if (part.isEmpty()) throw new IllegalArgumentException("Empty value in " + kind.unit + " field '" + text + "'");
            var item = parseItem(part, kind, flavor);
            items.add(item);
            if (item instanceof Values v) addValues(v, kind, flavor, values);
        }
        var any = items.stream().anyMatch(i -> i instanceof Values v && v.all && v.step == 1);
        return new Field(kind, any, items, values);
    }

    private static Field allValues(Kind kind, Flavor flavor) {
        var all = new Values(true, minValue(kind, flavor), kind.max, 1);
        var values = new BitSet();
        addValues(all, kind, flavor, values);
        return new Field(kind, true, List.of(all), values);
    }

    private static Item parseItem(String part, Kind kind, Flavor flavor) {
        if (kind == Kind.DAY_OF_MONTH) {
            if (part.equals("L")) return new LastDayOfMonth(0);
            if (part.equals("LW")) return new LastWeekdayOfMonth();
            if (part.startsWith("L-")) return new LastDayOfMonth(number(part.substring(2), kind, 0, 30));
            if (part.endsWith("W")) return new NearestWeekday(number(part.substring(0, part.length() - 1), kind, 1, 31));
        }
        if (kind == Kind.DAY_OF_WEEK) {
            if (part.equals("L")) return new LastDayOfWeek(flavor == Flavor.QUARTZ ? DayOfWeek.SATURDAY : DayOfWeek.SUNDAY);
            if (part.endsWith("L")) return new LastDayOfWeek(dayOfWeek(part.substring(0, part.length() - 1), flavor));
            var hash = part.indexOf('#');
            if (hash >= 0) {
                var day = dayOfWeek(part.substring(0, hash), flavor);
                var n = parseInt(part.substring(hash + 1), "occurrence in '" + part + "'");
                if (n < 1 || n > 5) throw new IllegalArgumentException("Occurrence in '" + part + "' must be between 1 and 5");
                return new NthDayOfWeek(day, n);
            }
        }

        var slash = part.indexOf('/');
        var range = slash >= 0 ? part.substring(0, slash) : part;
        var step = 1;
        if (slash >= 0) {
            step = parseInt(part.substring(slash + 1), "step in '" + part + "'");
            if (step < 1) throw new IllegalArgumentException("Step in '" + part + "' must be positive");
        }

        var min = minValue(kind, flavor);
        var max = kind.max;
        if (range.equals("*")) return new Values(true, min, max, step);

        var dash = range.indexOf('-', 1);
        if (dash >= 0) {
            var from = value(range.substring(0, dash), kind, flavor);
            var to = value(range.substring(dash + 1), kind, flavor);
            return new Values(false, from, to, step);
        }
        var from = value(range, kind, flavor);
        // "10/5" runs from 10 to the end of the range
        return new Values(false, from, slash >= 0 ? max : from, step);
    }

    /** Adds what {@code v} matches; days of the week are mapped to 1-7 from Monday. Ranges like {@code FRI-MON} wrap. */
    private static void addValues(Values v, Kind kind, Flavor flavor, BitSet values) {
        var min = minValue(kind, flavor);
        var max = kind.max;
        var size = max - min + 1;
        var length = v.from <= v.to ? v.to - v.from : v.to - v.from + size;
        for (int offset = 0; offset <= length; offset += v.step) {
            var value = min + (v.from - min + offset) % size;
            values.set(kind == Kind.DAY_OF_WEEK ? toDayOfWeek(value, flavor).getValue() : value);
        }
    }

    private static int minValue(Kind kind, Flavor flavor) {
        return kind == Kind.DAY_OF_WEEK && flavor == Flavor.QUARTZ ? 1 : kind.min;
    }

    private static int value(String text, Kind kind, Flavor flavor) {
        if (kind == Kind.MONTH) {
            var index = indexOf(MONTHS, text);
            if (index >= 0) return index + 1;
        }
        if (kind == Kind.DAY_OF_WEEK) {
            var index = indexOf(DAYS, text);
            if (index >= 0) return fromDayOfWeek(DayOfWeek.of(index + 1), flavor);
        }
        return number(text, kind, minValue(kind, flavor), kind.max);
    }

    private static DayOfWeek dayOfWeek(String text, Flavor flavor) {
        return toDayOfWeek(value(text, Kind.DAY_OF_WEEK, flavor), flavor);
    }

    private static DayOfWeek toDayOfWeek(int value, Flavor flavor) {
        if (flavor == Flavor.QUARTZ) return value == 1 ? DayOfWeek.SUNDAY : DayOfWeek.of(value - 1);
        return value == 0 ? DayOfWeek.SUNDAY : DayOfWeek.of(value);
    }

    private static int fromDayOfWeek(DayOfWeek day, Flavor flavor) {
        if (flavor == Flavor.QUARTZ) return day == DayOfWeek.SUNDAY ? 1 : day.getValue() + 1;
        return day.getValue();
    }

    private static int number(String text, Kind kind, int min, int max) {
        var value = parseInt(text, kind.unit + " '" + text + "'");
        if (value < min || value > max) {
            throw new IllegalArgumentException("Invalid " + kind.unit + " " + value + ": must be between " + min + " and " + max);
        }
        return value;
    }

    private static int parseInt(String text, String what) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + what);
        }
    }

    private static int indexOf(String[] names, String text) {
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(text)) return i;
        }
        return -1;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Next runs

    /** The first run strictly after {@code after}, in its time zone; empty if the cron never runs again. */
    public Optional<ZonedDateTime> nextExecution(ZonedDateTime after) {
        var zone = after.getZone();
        var start = after.toLocalDateTime().truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
        var date = start.toLocalDate();
        var from = start.toLocalTime();
        var limit = date.plusYears(MAX_YEARS_AHEAD);

        while (date.isBefore(limit)) {
            if (years != null && !years.values.get(date.getYear())) {
                var year = years.values.nextSetBit(date.getYear());
                if (year < 0) return Optional.empty();
                date = LocalDate.of(year, 1, 1);
                from = LocalTime.MIDNIGHT;
                continue;
            }
            if (!months.values.get(date.getMonthValue())) {
                date = date.withDayOfMonth(1).plusMonths(1);
                from = LocalTime.MIDNIGHT;
                continue;
            }
            if (matchesDay(date)) {
                var time = firstTimeFrom(from);
                if (time != null) {
                    var result = ZonedDateTime.of(date, time, zone);
                    if (result.isAfter(after)) return Optional.of(result);
                    // Clocks went back and this local time already passed; look further on the same day
                    if (time.getHour() < 23 || time.getMinute() < 59 || time.getSecond() < 59) {
                        from = time.plusSeconds(1);
                        continue;
                    }
                }
            }
            date = date.plusDays(1);
            from = LocalTime.MIDNIGHT;
        }
        return Optional.empty();
    }

    private @Nullable LocalTime firstTimeFrom(LocalTime from) {
        for (int h = hours.values.nextSetBit(from.getHour()); h >= 0; h = hours.values.nextSetBit(h + 1)) {
            var sameHour = h == from.getHour();
            for (int m = minutes.values.nextSetBit(sameHour ? from.getMinute() : 0); m >= 0;
                 m = minutes.values.nextSetBit(m + 1)) {
                var s = seconds.values.nextSetBit(sameHour && m == from.getMinute() ? from.getSecond() : 0);
                if (s >= 0) return LocalTime.of(h, m, s);
            }
        }
        return null;
    }

    private boolean matchesDay(LocalDate date) {
        return matches(daysOfMonth, date) && matches(daysOfWeek, date);
    }

    private static boolean matches(Field field, LocalDate date) {
        if (field.any) return true;
        var value = field.kind == Kind.DAY_OF_WEEK ? date.getDayOfWeek().getValue() : date.getDayOfMonth();
        if (field.values.get(value)) return true;

        var length = date.lengthOfMonth();
        for (var item : field.items) {
            var matches = switch (item) {
                case Values ignored -> false;
                case LastDayOfMonth last -> date.getDayOfMonth() == length - last.offset;
                case LastWeekdayOfMonth ignored -> date.equals(nearestWeekday(YearMonth.from(date), length));
                case NearestWeekday nearest -> date.equals(nearestWeekday(YearMonth.from(date), nearest.day));
                case LastDayOfWeek last -> date.getDayOfWeek() == last.day && date.getDayOfMonth() + 7 > length;
                case NthDayOfWeek nth -> date.getDayOfWeek() == nth.day && (date.getDayOfMonth() - 1) / 7 + 1 == nth.n;
            };
            if (matches) return true;
        }
        return false;
    }

    /** Quartz's {@code W}: the nearest weekday that is still in the same month. */
    private static @Nullable LocalDate nearestWeekday(YearMonth month, int day) {
        var length = month.lengthOfMonth();
        if (day > length) return null;
        var date = month.atDay(day);
        return switch (date.getDayOfWeek()) {
            case SATURDAY -> day == 1 ? date.plusDays(2) : date.minusDays(1);
            case SUNDAY -> day == length ? date.minusDays(2) : date.plusDays(1);
            default -> date;
        };
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Description

    /** E.g. "At 09:30, Monday through Friday" or "Every 15 minutes, between 09:00 and 17:59". */
    public String describe() {
        List<String> parts = new ArrayList<>();
        var fixedTimes = describeTime(parts);

        var dayOfMonth = describeDayOfMonth();
        var dayOfWeek = describeDayOfWeek();
        var month = describeMonth();
        if (dayOfMonth != null) parts.add(dayOfMonth);
        if (dayOfWeek != null) parts.add(dayOfWeek);
        if (month != null) parts.add(month);
        if (fixedTimes && dayOfMonth == null && dayOfWeek == null) parts.add("every day");
        if (years != null && !years.any) parts.add("in " + list(years, Integer::toString));

        var text = String.join(", ", parts);
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** @return whether the cron runs at fixed times of day, so "every day" makes sense after it */
    private boolean describeTime(List<String> parts) {
        if (seconds.isValueList() && minutes.isValueList() && hours.isValueList()) {
            var count = seconds.values.cardinality() * minutes.values.cardinality() * hours.values.cardinality();
            if (count <= 6) {
                var withSeconds = !(seconds.isSingleValue() && seconds.single() == 0);
                List<String> times = new ArrayList<>();
                hours.values.stream().forEach(h -> minutes.values.stream().forEach(m -> seconds.values.stream().forEach(s ->
                        times.add(withSeconds ? "%02d:%02d:%02d".formatted(h, m, s) : "%02d:%02d".formatted(h, m)))));
                parts.add("at " + joinAnd(times));
                return true;
            }
        }

        if (seconds.isSingleValue() && minutes.isSingleValue()) {
            var second = seconds.single();
            var minute = minutes.single();
            var past = second == 0 ? "minute " + minute : "%02d:%02d".formatted(minute, second);
            if (hours.any) {
                parts.add(minute == 0 && second == 0 ? "every hour" : "every hour at " + past);
                return false;
            }
            if (hours.items.size() == 1 && hours.items.getFirst() instanceof Values(
                    boolean all, int from, int v1, int step
            ) && from != v1) {
                var time = second == 0 ? "%02d:%02d" : "%02d:%02d:" + "%02d".formatted(second);
                var every = step == 1 ? "every hour" : "every " + step + " hours";
                if (all) {
                    parts.add(every + (minute == 0 && second == 0 ? "" : " at " + past));
                } else {
                    parts.add(every + " from " + time.formatted(from, minute) + " to " + time.formatted(v1, minute));
                }
                return false;
            }
            parts.add("at " + past + " past hour " + list(hours, Integer::toString));
            return false;
        }

        var second = describeUnit(seconds);
        if (second != null) parts.add(second);
        // "every 15 seconds" already says it runs every minute
        if (!minutes.any || seconds.isValueList()) {
            var minute = describeUnit(minutes);
            if (minute != null) parts.add(minute);
        }
        if (!hours.any) {
            if (hours.items.size() == 1 && hours.items.getFirst() instanceof Values v && v.step == 1) {
                parts.add("between %02d:00 and %02d:59".formatted(v.from, v.to));
            } else if (hours.items.size() == 1 && hours.items.getFirst() instanceof Values v && v.all) {
                parts.add("every " + v.step + " hours");
            } else {
                parts.add("during hour " + list(hours, Integer::toString));
            }
        }
        return false;
    }

    /** Seconds or minutes when they aren't a single value, e.g. "every 15 minutes"; {@code null} for second 0. */
    private static @Nullable String describeUnit(Field field) {
        var unit = field.kind.unit;
        if (field.isSingleValue() && field.single() == 0 && field.kind == Kind.SECOND) return null;
        if (field.any) return "every " + unit;
        if (field.items.size() == 1 && field.items.getFirst() instanceof Values v && v.step > 1) {
            var text = "every " + v.step + " " + unit + "s";
            if (v.from != field.kind.min) text += " from " + unit + " " + v.from;
            if (v.to != field.kind.max) text += " to " + v.to;
            return text;
        }
        return "at " + unit + " " + list(field, Integer::toString);
    }

    private @Nullable String describeDayOfMonth() {
        if (daysOfMonth.any) return null;
        if (daysOfMonth.items.size() == 1 && daysOfMonth.items.getFirst() instanceof Values v && v.step > 1) {
            return "every " + v.step + " days" + (v.from == 1 ? "" : " from day " + v.from);
        }
        List<String> texts = new ArrayList<>();
        for (var item : daysOfMonth.items) {
            texts.add(switch (item) {
                case LastDayOfMonth last when last.offset == 0 -> "the last day";
                case LastDayOfMonth last -> last.offset + (last.offset == 1 ? " day" : " days") + " before the last day";
                case LastWeekdayOfMonth ignored -> "the last weekday";
                case NearestWeekday nearest -> "the weekday nearest day " + nearest.day;
                case Values v -> "day " + range(v, Integer::toString);
                default -> throw new IllegalStateException(item.toString());
            });
        }
        return "on " + joinAnd(texts) + " of the month";
    }

    private @Nullable String describeDayOfWeek() {
        if (daysOfWeek.any) return null;
        List<String> texts = new ArrayList<>();
        var ofMonth = false;
        for (var item : daysOfWeek.items) {
            switch (item) {
                case LastDayOfWeek last -> {
                    texts.add("the last " + name(last.day));
                    ofMonth = true;
                }
                case NthDayOfWeek nth -> {
                    texts.add("the " + ORDINALS[nth.n - 1] + " " + name(nth.day));
                    ofMonth = true;
                }
                case Values v -> texts.add(range(v, value -> name(toDayOfWeek(value, flavor))));
                default -> throw new IllegalStateException(item.toString());
            }
        }
        return "on " + joinAnd(texts) + (ofMonth ? " of the month" : "");
    }

    private @Nullable String describeMonth() {
        if (months.any) return null;
        if (months.items.size() == 1 && months.items.getFirst() instanceof Values v && v.step > 1) {
            return "every " + v.step + " months" + (v.from == 1 ? "" : " from " + monthName(v.from));
        }
        return "in " + list(months, CronExpression::monthName);
    }

    private static String list(Field field, IntFunction<String> name) {
        List<String> texts = new ArrayList<>();
        for (var item : field.items) {
            if (item instanceof Values v) texts.add(range(v, name));
        }
        return joinAnd(texts);
    }

    private static String range(Values v, IntFunction<String> name) {
        var text = v.from == v.to ? name.apply(v.from) : name.apply(v.from) + " through " + name.apply(v.to);
        return v.step > 1 ? "every " + v.step + " from " + text : text;
    }

    private static String joinAnd(List<String> texts) {
        if (texts.size() <= 1) return String.join("", texts);
        return String.join(", ", texts.subList(0, texts.size() - 1)) + " and " + texts.getLast();
    }

    private static String name(DayOfWeek day) {
        return day.getDisplayName(TextStyle.FULL, Locale.UK);
    }

    private static String monthName(int month) {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.UK);
    }
}
