package com.kivojenko.plugin.manage_scheduled_events.ui.tree.node;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

/**
 * A Quartz job ({@code org.quartz.Job} / {@code QuartzJobBean}) with one of its cron triggers. A job without a cron
 * trigger found in source is listed once with no cron; a cron trigger whose job could not be resolved is listed under
 * the class that defines it.
 */
public class QuartzJobNode extends JobNode {
    public static final String NO_TRIGGER = "(no cron trigger)";

    static final CronParser parser =
            new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

    private final Project project;
    private final @Nullable SmartPsiElementPointer<PsiClass> jobPointer;
    private final @Nullable SmartPsiElementPointer<PsiExpression> cronPointer;
    private final String name;

    public static QuartzJobNode ofJob(PsiClass job) {
        return new QuartzJobNode(job.getProject(), job, null);
    }

    /**
     * @param cron the cron argument of the trigger, e.g. the string passed to {@code cronSchedule(...)}, a
     *             {@code @Value} field or a {@code @ConfigurationProperties} getter.
     */
    public static QuartzJobNode ofTrigger(@Nullable PsiClass job, PsiExpression cron) {
        return new QuartzJobNode(cron.getProject(), job, cron);
    }

    private QuartzJobNode(Project project, @Nullable PsiClass job, @Nullable PsiExpression cron) {
        this(project, job, cron, triggerName(cron));
    }

    private QuartzJobNode(Project project, @Nullable PsiClass job, @Nullable PsiExpression cron, String name) {
        super(name);
        var pointerManager = SmartPointerManager.getInstance(project);
        this.project = project;
        this.jobPointer = job == null ? null : pointerManager.createSmartPsiElementPointer(job);
        this.cronPointer = cron == null ? null : pointerManager.createSmartPsiElementPointer(cron);
        this.name = name;
    }

    /** Triggers are usually defined in a {@code @Bean} method, whose name is the trigger's bean name. */
    private static String triggerName(@Nullable PsiExpression cron) {
        if (cron == null) return NO_TRIGGER;
        var method = PsiTreeUtil.getParentOfType(cron, PsiMethod.class);
        return method != null ? method.getName() : "trigger";
    }

    public @Nullable PsiClass getJob() {
        return jobPointer == null ? null : ReadAction.compute(jobPointer::getElement);
    }

    public @Nullable PsiExpression getCronExpression() {
        return cronPointer == null ? null : ReadAction.compute(cronPointer::getElement);
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public @Nullable PsiElement getNavigationTarget() {
        var cron = getCronExpression();
        return cron != null ? cron : getJob();
    }

    @Override
    protected CronParser getParser() {
        return parser;
    }

    @Override
    protected @Nullable PsiElement getCronElement() {
        return getCronExpression();
    }

    /** Quartz takes the string as is; properties reach it through {@code @Value} or getters instead. */
    @Override
    protected boolean resolvesPlaceholders() {
        return false;
    }

    @Override
    public String getFilePath() {
        return ReadAction.compute(() -> {
            PsiElement source = jobPointer != null ? jobPointer.getElement() : null;
            if (source == null && cronPointer != null) source = cronPointer.getElement();
            return source != null && source.getContainingFile() instanceof PsiJavaFile file ? file.getPackageName() : "";
        });
    }

    @Override
    public String getClassName() {
        return ReadAction.compute(() -> {
            PsiClass cls = jobPointer != null ? jobPointer.getElement() : null;
            if (cls == null && cronPointer != null) {
                cls = PsiTreeUtil.getParentOfType(cronPointer.getElement(), PsiClass.class);
            }
            return cls != null && cls.getName() != null ? cls.getName() : "";
        });
    }
}
