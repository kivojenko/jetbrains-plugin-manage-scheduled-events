package com.kivojenko.plugin.manage_scheduled_events.ui.tree.node;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.kivojenko.plugin.manage_scheduled_events.cron.CronExpression;
import com.kivojenko.plugin.manage_scheduled_events.service.CronResolver;
import com.kivojenko.plugin.manage_scheduled_events.service.CronValue;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** A scheduled job shown in the tree: a Spring {@code @Scheduled} method or a Quartz job trigger. */
public abstract class JobNode extends TypedNode {
    public static final String NO_CRON = CronResolver.NO_CRON;

    private volatile @Nullable CronValue cronValue;

    protected JobNode(String name) {
        super(name, NodeType.METHOD);
    }

    public abstract Project getProject();

    public abstract String getName();

    public abstract String getClassName();

    public abstract String getFilePath();

    /** Element to open on double-click, or {@code null} if it no longer exists. */
    public abstract @Nullable PsiElement getNavigationTarget();

    /** The source element holding the cron, e.g. the {@code cron} attribute value; {@code null} if there is none. */
    protected abstract @Nullable PsiElement getCronElement();

    /** Whether {@code ${...}} in the cron string is a Spring placeholder. */
    protected abstract boolean resolvesPlaceholders();

    /** Spring and Quartz crons differ, e.g. Quartz allows a year field. */
    protected abstract CronExpression.Flavor getCronFlavor();

    private CronExpression parseCron() {
        return CronExpression.parse(getCron(), getCronFlavor());
    }

    /**
     * Resolves the cron again from source and config files. Uses indexes, so it must run in a background read action;
     * the tree only ever shows the last result.
     */
    public void resolveCron() {
        CronValue value;
        try {
            value = computeCron();
        } catch (IndexNotReadyException e) {
            value = CronValue.unresolved(NO_CRON, "Waiting for indexing to finish");
        }
        cronValue = value;
    }

    /** Looks the cron up from source; runs in {@link #resolveCron()}. */
    protected CronValue computeCron() {
        return CronResolver.resolve(getCronElement(), resolvesPlaceholders());
    }

    /** {@code false} for crons that can't be written back, e.g. read from a database. */
    public boolean isEditable() {
        return true;
    }

    public boolean isResolved() {
        return cronValue != null;
    }

    /** The last {@link #resolveCron()} result, or {@link CronValue#PENDING} before the first one. */
    public CronValue getCronValue() {
        var value = cronValue;
        return value != null ? value : CronValue.PENDING;
    }

    public String getCron() {
        return getCronValue().cron();
    }

    /** Where the cron comes from, if not from the code itself, e.g. {@code jobs.report.cron (prod overrides)}. */
    public @Nullable String getCronOrigin() {
        var value = getCronValue();
        if (value.propertyKey() == null) return null;
        if (value.overridingProfiles().isEmpty()) return value.propertyKey();
        return value.propertyKey() + " (" + String.join(", ", value.overridingProfiles()) + " overrides)";
    }

    /**
     * Writes the cron where it comes from: a string literal, a property in application.yml / .properties, or a
     * placeholder's default. Where to write is looked up in the background, then written on the UI thread.
     *
     * @param done called on the UI thread with {@code false} if the cron could not be written, e.g. it comes from a
     *             shared constant or its source was removed since this node was created
     */
    public void setCron(String newCron, Consumer<Boolean> done) {
        if (Objects.equals(newCron, getCron())) {
            done.accept(true);
            return;
        }

        var project = getProject();
        ReadAction.nonBlocking(() -> CronResolver.findTarget(getCronElement(), resolvesPlaceholders()))
                .inSmartMode(project)
                .finishOnUiThread(ModalityState.defaultModalityState(), target -> done.accept(target != null
                        && WriteCommandAction.writeCommandAction(project).compute(() -> target.write(newCron))))
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    /** {@code false} for e.g. {@code @Scheduled(fixedRate)} methods and Quartz jobs without a cron trigger. */
    public boolean hasCron() {
        return !NO_CRON.equals(getCron());
    }

    /** Should be flagged: the cron couldn't be resolved, or it is there but invalid. Having no cron is fine. */
    public boolean isBroken() {
        return isResolved() && (getCronValue().problem() != null || (hasCron() && !isValid()));
    }

    public boolean isValid() {
        if (getCronValue().problem() != null) return false;
        try {
            parseCron();
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    /**
     * Average time between the next few runs, so irregular crons (e.g. only on weekdays) still compare sensibly.
     * Empty if the cron is invalid or never runs again.
     */
    public Optional<Duration> getAverageInterval() {
        try {
            var execution = parseCron();
            var first = execution.nextExecution(ZonedDateTime.now());
            if (first.isEmpty()) return Optional.empty();

            var last = first.get();
            int intervals = 0;
            for (; intervals < 10; intervals++) {
                var next = execution.nextExecution(last);
                if (next.isEmpty()) break;
                last = next.get();
            }
            return intervals == 0 ? Optional.empty() : Optional.of(Duration.between(first.get(), last).dividedBy(intervals));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String getCronDescription() {
        var problem = getCronValue().problem();
        if (!isResolved()) return "Resolving…";
        if (problem != null) return problem;
        if (!hasCron()) {
            var source = getCronValue().runtimeSource();
            return source != null ? "Set at runtime from " + source : "No cron schedule";
        }
        try {
            return parseCron().describe();
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
}
