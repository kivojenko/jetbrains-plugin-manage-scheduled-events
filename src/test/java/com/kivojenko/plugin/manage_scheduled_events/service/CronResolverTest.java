package com.kivojenko.plugin.manage_scheduled_events.service;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

import java.util.List;

public class CronResolverTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addClass("""
                package org.springframework.scheduling.annotation;
                public @interface Scheduled { String cron() default ""; }""");
        myFixture.addClass("""
                package org.springframework.beans.factory.annotation;
                public @interface Value { String value(); }""");
        myFixture.addClass("""
                package org.springframework.boot.context.properties;
                public @interface ConfigurationProperties { String value() default ""; String prefix() default ""; }""");
    }

    public void testPlaceholderFromNestedYaml() {
        myFixture.addFileToProject("application.yml", """
                jobs:
                  backup:
                    cron: "0 0 3 * * *"
                """);
        var value = resolve(scheduledCron("${jobs.backup.cron}"), true);

        assertEquals("0 0 3 * * *", value.cron());
        assertEquals("jobs.backup.cron", value.propertyKey());
        assertNull(value.problem());
    }

    public void testProfileOverridesAreReported() {
        myFixture.addFileToProject("application.yml", """
                jobs.backup.cron: "0 0 3 * * *"
                ---
                spring:
                  config:
                    activate:
                      on-profile: staging
                jobs:
                  backup:
                    cron: "0 0 * * * *"
                """);
        myFixture.addFileToProject("application-prod.properties", "jobs.backup.cron=0 0/5 * * * *");
        var value = resolve(scheduledCron("${jobs.backup.cron}"), true);

        assertEquals("0 0 3 * * *", value.cron());
        assertSameElements(value.overridingProfiles(), List.of("staging", "prod"));
    }

    public void testPropertiesWinOverYaml() {
        myFixture.addFileToProject("application.yml", "jobs.backup.cron: '0 0 3 * * *'");
        myFixture.addFileToProject("application.properties", "jobs.backup.cron=0 0 4 * * *");

        assertEquals("0 0 4 * * *", resolve(scheduledCron("${jobs.backup.cron}"), true).cron());
    }

    public void testPlaceholderDefaultAndMissingKey() {
        assertEquals("0 0 7 * * *", resolve(scheduledCron("${jobs.digest.cron:0 0 7 * * *}"), true).cron());

        var missing = resolve(scheduledCron("${jobs.missing.cron}"), true);
        assertNotNull(missing.problem());
        assertEquals("jobs.missing.cron", missing.propertyKey());
    }

    public void testPlaceholderIgnoredWhereSpringDoesNotResolveIt() {
        var value = resolve(scheduledCron("${jobs.backup.cron}"), false);

        assertEquals("${jobs.backup.cron}", value.cron());
        assertNull(value.propertyKey());
    }

    public void testValueAnnotatedParameter() {
        myFixture.addFileToProject("application.yml", "jobs:\n  archive-cron: \"0 0 4 ? * SUN\"\n");
        var cls = myFixture.addClass("""
                import org.springframework.beans.factory.annotation.Value;
                class Config {
                    void trigger(@Value("${jobs.archive-cron}") String cron) { schedule(cron); }
                    void schedule(String cron) {}
                }""");

        assertEquals("0 0 4 ? * SUN", resolve(firstArgument(cls, "schedule"), false).cron());
    }

    public void testConfigurationPropertiesGetterChain() {
        myFixture.addFileToProject("application.yml", "jobs:\n  report:\n    cron: \"0 0/15 9-17 ? * MON-FRI\"\n");
        myFixture.addClass("""
                import org.springframework.boot.context.properties.ConfigurationProperties;
                @ConfigurationProperties(prefix = "jobs")
                class JobProperties {
                    Report getReport() { return null; }
                    static class Report { String getCron() { return null; } }
                }""");
        var cls = myFixture.addClass("""
                class Config {
                    void trigger(JobProperties jobs) { schedule(jobs.getReport().getCron()); }
                    void schedule(String cron) {}
                }""");

        var value = resolve(firstArgument(cls, "schedule"), false);
        assertEquals("0 0/15 9-17 ? * MON-FRI", value.cron());
        assertEquals("jobs.report.cron", value.propertyKey());
    }

    public void testArgumentTracedThroughSingleCaller() {
        var cls = myFixture.addClass("""
                class Config {
                    void createJob() { trigger("0 0 2 * * ?"); }
                    void trigger(String cron) { Runnable r = () -> schedule(cron); }
                    void schedule(String cron) {}
                }""");
        var cron = firstArgument(cls, "schedule");

        assertEquals("0 0 2 * * ?", resolve(cron, false).cron());
        assertTrue(WriteCommandAction.writeCommandAction(getProject())
                .compute(() -> CronResolver.write(cron, false, "0 0 3 * * ?")));
        assertTrue(cls.getText().contains("trigger(\"0 0 3 * * ?\")"));
    }

    public void testRuntimeCronReportsSource() {
        var cls = myFixture.addClass("""
                class Config {
                    interface Job { String getCron(); }
                    void createJob(Job job) { trigger(job.getCron()); }
                    void trigger(String cron) { schedule(cron); }
                    void schedule(String cron) {}
                }""");
        var value = resolve(firstArgument(cls, "schedule"), false);

        assertEquals(CronResolver.NO_CRON, value.cron());
        assertEquals("job.getCron()", value.runtimeSource());
        assertNull(value.problem());
    }

    public void testArgumentNotTracedThroughSeveralCallers() {
        var cls = myFixture.addClass("""
                class Config {
                    void first() { trigger("0 0 2 * * ?"); }
                    void second() { trigger("0 0 4 * * ?"); }
                    void trigger(String cron) { schedule(cron); }
                    void schedule(String cron) {}
                }""");

        assertEquals("cron", resolve(firstArgument(cls, "schedule"), false).runtimeSource());
    }

    public void testWriteUpdatesYamlValue() {
        var yaml = myFixture.addFileToProject("application.yml", "jobs:\n  backup:\n    cron: \"0 0 3 * * *\"\n");
        var cron = scheduledCron("${jobs.backup.cron}");

        assertTrue(write(cron, "* 0 5 * * *"));
        assertEquals("jobs:\n  backup:\n    cron: \"* 0 5 * * *\"\n", yaml.getText());
        assertEquals("${jobs.backup.cron}", ((com.intellij.psi.PsiLiteralExpression) cron).getValue());
    }

    public void testWriteUpdatesPropertiesValue() {
        var properties = myFixture.addFileToProject("application.properties", "jobs.backup.cron=0 0 3 * * *");

        assertTrue(write(scheduledCron("${jobs.backup.cron}"), "0 0 5 * * *"));
        assertEquals("jobs.backup.cron=0 0 5 * * *", properties.getText());
    }

    public void testWriteUpdatesPlaceholderDefaultWhenNotConfigured() {
        var cron = scheduledCron("${jobs.digest.cron:0 0 7 * * *}");

        assertTrue(write(cron, "0 0 8 * * *"));
        assertEquals("0 0 8 * * *", resolve(scheduledCron("${jobs.digest.cron:0 0 8 * * *}"), true).cron());
        assertTrue(myFixture.getFile().getText().contains("${jobs.digest.cron:0 0 8 * * *}"));
    }

    public void testWriteRefusesMissingPropertyWithoutDefault() {
        assertFalse(write(scheduledCron("${jobs.missing.cron}"), "0 0 8 * * *"));
    }

    private PsiElement scheduledCron(String cron) {
        myFixture.configureByText("Jobs.java", """
                import org.springframework.scheduling.annotation.Scheduled;
                class Jobs {
                    @Scheduled(cron = "%s")
                    void run() {}
                }""".formatted(cron));
        var cls = ((com.intellij.psi.PsiJavaFile) myFixture.getFile()).getClasses()[0];
        return cls.getMethods()[0].getModifierList().getAnnotations()[0].findDeclaredAttributeValue("cron");
    }

    private static PsiElement firstArgument(PsiClass cls, String methodName) {
        return PsiTreeUtil.findChildrenOfType(cls, PsiMethodCallExpression.class).stream()
                .filter(call -> methodName.equals(call.getMethodExpression().getReferenceName()))
                .findFirst()
                .orElseThrow()
                .getArgumentList()
                .getExpressions()[0];
    }

    private static CronValue resolve(PsiElement element, boolean placeholders) {
        return CronResolver.resolve(element, placeholders);
    }

    private boolean write(PsiElement element, String cron) {
        return WriteCommandAction.writeCommandAction(getProject()).compute(() -> CronResolver.write(element, true, cron));
    }
}
