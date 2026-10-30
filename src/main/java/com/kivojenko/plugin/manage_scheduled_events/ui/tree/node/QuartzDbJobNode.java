package com.kivojenko.plugin.manage_scheduled_events.ui.tree.node;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.kivojenko.plugin.manage_scheduled_events.cron.CronExpression;
import com.kivojenko.plugin.manage_scheduled_events.service.CronValue;
import com.kivojenko.plugin.manage_scheduled_events.service.QuartzTrigger;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A Quartz job read from the {@code QRTZ_} tables, e.g. one of many jobs an application creates at runtime for the
 * same job class. Read-only: Quartz applications usually recreate their triggers on startup.
 */
public class QuartzDbJobNode extends JobNode {
    private static final DateTimeFormatter NEXT_FIRE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Project project;
    @Getter
    private final QuartzTrigger trigger;
    private final @Nullable SmartPsiElementPointer<PsiClass> jobPointer;

    /** @param job the job class in source, {@code null} if it isn't in the project */
    public QuartzDbJobNode(Project project, QuartzTrigger trigger, @Nullable PsiClass job) {
        super(trigger.jobName());
        this.project = project;
        this.trigger = trigger;
        this.jobPointer = job == null ? null : SmartPointerManager.getInstance(project).createSmartPsiElementPointer(job);
    }

    @Override
    public Project getProject() {
        return project;
    }

    /** The job, and the trigger too when a job has a differently named one. */
    @Override
    public String getName() {
        var triggerName = trigger.triggerName();
        return triggerName == null || triggerName.equals(trigger.jobName())
                ? trigger.jobName()
                : trigger.jobName() + " / " + triggerName;
    }

    @Override
    public String getClassName() {
        var name = trigger.jobClassName();
        return name.substring(name.lastIndexOf('.') + 1).replace('$', '.');
    }

    @Override
    public String getFilePath() {
        var name = trigger.jobClassName();
        return name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : "";
    }

    @Override
    public @Nullable PsiElement getNavigationTarget() {
        return jobPointer == null ? null : ReadAction.compute(jobPointer::getElement);
    }

    @Override
    protected @Nullable PsiElement getCronElement() {
        return null;
    }

    @Override
    protected boolean resolvesPlaceholders() {
        return false;
    }

    @Override
    protected CronExpression.Flavor getCronFlavor() {
        return CronExpression.Flavor.QUARTZ;
    }

    @Override
    protected CronValue computeCron() {
        return CronValue.of(trigger.cron() != null ? trigger.cron() : NO_CRON);
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public void setCron(String newCron, Consumer<Boolean> done) {
        done.accept(false);
    }

    @Override
    public String getCronDescription() {
        if (trigger.cron() != null) return super.getCronDescription();
        if (trigger.triggerName() == null) return "No trigger in database";
        if (trigger.repeatIntervalMs() != null) {
            return "Every " + StringUtil.formatDuration(trigger.repeatIntervalMs()) + " (simple trigger)";
        }
        return StringUtil.toLowerCase(String.valueOf(trigger.triggerType())) + " trigger";
    }

    /** E.g. {@code PAUSED · next 2026-10-30 01:35 · medre@localhost}. */
    @Override
    public String getCronOrigin() {
        List<String> parts = new ArrayList<>();
        if (trigger.triggerState() != null) parts.add(trigger.triggerState());
        if (trigger.nextFireTime() != null) {
            parts.add("next " + NEXT_FIRE.format(Instant.ofEpochMilli(trigger.nextFireTime()).atZone(ZoneId.systemDefault())));
        }
        parts.add(trigger.dataSource());
        return String.join(" · ", parts);
    }
}
