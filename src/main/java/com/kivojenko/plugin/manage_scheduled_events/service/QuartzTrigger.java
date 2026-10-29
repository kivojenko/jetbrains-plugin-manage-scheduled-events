package com.kivojenko.plugin.manage_scheduled_events.service;

import org.jetbrains.annotations.Nullable;

/**
 * A Quartz job as stored in the {@code QRTZ_} tables, with one of its triggers.
 *
 * @param jobClassName     binary name, e.g. {@code com.example.Jobs$ReportJob}
 * @param triggerName      {@code null} if the job has no trigger
 * @param triggerType      e.g. {@code CRON} or {@code SIMPLE}
 * @param triggerState     e.g. {@code WAITING} or {@code PAUSED}
 * @param nextFireTime     epoch millis, {@code null} if it never fires again
 * @param cron             set for cron triggers
 * @param repeatIntervalMs set for simple triggers
 * @param dataSource       name of the data source it was read from
 */
public record QuartzTrigger(String jobName, String jobGroup, String jobClassName,
                            @Nullable String triggerName, @Nullable String triggerType, @Nullable String triggerState,
                            @Nullable Long nextFireTime, @Nullable String cron, @Nullable Long repeatIntervalMs,
                            String dataSource) {
}
