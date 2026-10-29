package com.kivojenko.plugin.manage_scheduled_events.service;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A cron as it is resolved from source.
 *
 * @param cron             the effective cron for the default profile
 * @param propertyKey      the Spring property it was read from, if any
 * @param overridingProfiles profiles that set the property to something else
 * @param problem          why the cron could not be resolved, if it couldn't
 * @param runtimeSource    the expression the cron is computed from at runtime, e.g. {@code job.getScheduleCron()},
 *                         when it can't be known from source
 */
public record CronValue(String cron, @Nullable String propertyKey, List<String> overridingProfiles,
                        @Nullable String problem, @Nullable String runtimeSource) {

    /** Shown until the cron has been resolved in the background. */
    public static final CronValue PENDING = new CronValue("", null, List.of(), null, null);

    public static CronValue of(String cron) {
        return new CronValue(cron, null, List.of(), null, null);
    }

    public static CronValue unresolved(String cron, String problem) {
        return new CronValue(cron, null, List.of(), problem, null);
    }

    public static CronValue runtime(String cron, String source) {
        return new CronValue(cron, null, List.of(), null, source);
    }

    /** Whether there is a cron at all, e.g. not {@code @Scheduled(fixedRate)} or a cron set at runtime. */
    public boolean hasCron() {
        return !CronResolver.NO_CRON.equals(cron);
    }
}
