package com.kivojenko.plugin.manage_scheduled_events.service;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.JobNode;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.QuartzDbJobNode;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.QuartzJobNode;

import java.util.List;

public class QuartzServiceTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addClass("package org.quartz; public interface Job {}");
        myFixture.addClass("package org.quartz; public interface ScheduleBuilder {}");
        myFixture.addClass("""
                package org.quartz;
                public class CronScheduleBuilder implements ScheduleBuilder {
                    public static CronScheduleBuilder cronSchedule(String cron) { return null; }
                }""");
        myFixture.addClass("""
                package org.quartz;
                public class JobBuilder { public static JobBuilder newJob(Class<? extends Job> job) { return null; } }""");
        myFixture.addClass("package jobs; public class LauncherJob implements org.quartz.Job {}");
        // Like an application that keeps crons in its own table and schedules every job with one job class
        myFixture.addClass("""
                package jobs;
                import org.quartz.*;
                import java.util.function.Supplier;
                class JobSchedulerService {
                    interface JobDefinition { String getCron(); }
                    private static ScheduleBuilder createSchedule(String cronExpression) {
                        return CronScheduleBuilder.cronSchedule(cronExpression);
                    }
                    void createScheduledJob(JobDefinition job) {
                        buildJobDetail();
                        cronBasedScheduler(job.getCron()).get();
                    }
                    private void buildJobDetail() { JobBuilder.newJob(LauncherJob.class); }
                    private Supplier<ScheduleBuilder> cronBasedScheduler(String cron) { return () -> createSchedule(cron); }
                }""");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            QuartzDatabaseService.getInstance(getProject()).setTriggers(List.of());
        } finally {
            super.tearDown();
        }
    }

    public void testTriggerLinkedToJobThroughCallers() {
        var jobs = findJobs();

        assertEquals(1, jobs.size());
        var trigger = (QuartzJobNode) jobs.getFirst();
        assertEquals("LauncherJob", trigger.getClassName());
        trigger.resolveCron();
        assertEquals("job.getCron()", trigger.getCronValue().runtimeSource());
        assertEquals("Set at runtime from job.getCron()", trigger.getCronDescription());
    }

    public void testDatabaseJobsReplaceTriggersWithoutCron() {
        QuartzDatabaseService.getInstance(getProject()).setTriggers(List.of(
                new QuartzTrigger("Nightly report", "app", "jobs.LauncherJob", "Nightly report", "CRON", "WAITING",
                        null, "0 30 1 * * ?", null, "app@localhost"),
                new QuartzTrigger("Residency check", "app", "jobs.LauncherJob", "Residency check", "SIMPLE", "PAUSED",
                        null, null, 900_000L, "app@localhost")));

        var jobs = findJobs();

        assertEquals(2, jobs.size());
        assertTrue(jobs.stream().allMatch(QuartzDbJobNode.class::isInstance));
        var report = jobs.getFirst();
        report.resolveCron();
        assertEquals("Nightly report", report.getName());
        assertEquals("LauncherJob", report.getClassName());
        assertEquals("0 30 1 * * ?", report.getCron());
        assertFalse(report.isEditable());
        assertEquals("WAITING · app@localhost", report.getCronOrigin());
        assertNotNull(report.getNavigationTarget());

        var residency = jobs.get(1);
        residency.resolveCron();
        assertFalse(residency.hasCron());
        assertEquals("Every 15 m (simple trigger)", residency.getCronDescription());
    }

    private List<JobNode> findJobs() {
        return getProject().getService(QuartzService.class).findQuartzJobs();
    }
}
