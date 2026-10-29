package com.kivojenko.plugin.manage_scheduled_events.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.JobNode;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.QuartzDbJobNode;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.QuartzJobNode;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Finds Quartz jobs and the cron triggers that schedule them. Quartz is looked up by name, so nothing is found unless
 * the project has Quartz on its classpath.
 * <p>
 * Jobs last read from a database ({@link QuartzDatabaseService}) are listed too. They replace what source can't tell
 * for the same job class: its triggers without a known cron, and the job itself when no trigger was found.
 */
@Service(Service.Level.PROJECT)
public record QuartzService(Project project) {
    private static final String JOB = "org.quartz.Job";
    private static final String CRON_EXPRESSION = "org.quartz.CronExpression";
    /** How many calls up from a trigger's method its job is looked for, e.g. {@code createSchedule ← createJob}. */
    private static final int MAX_CALLER_DEPTH = 3;

    /** Methods whose first argument is a cron expression. */
    private static final List<CronSetter> CRON_SETTERS = List.of(
            new CronSetter("org.quartz.CronScheduleBuilder", "cronSchedule"),
            new CronSetter("org.quartz.CronScheduleBuilder", "cronScheduleNonvalidatedExpression"),
            new CronSetter("org.quartz.impl.triggers.CronTriggerImpl", "setCronExpression"),
            new CronSetter("org.springframework.scheduling.quartz.CronTriggerFactoryBean", "setCronExpression")
    );

    private record CronSetter(String className, String methodName) {
    }

    @SneakyThrows
    public List<JobNode> findQuartzJobs() {
        return ApplicationManager.getApplication()
                .executeOnPooledThread(() -> ReadAction.compute(this::collectJobs))
                .get();
    }

    private List<JobNode> collectJobs() {
        var facade = JavaPsiFacade.getInstance(project);
        var libraries = GlobalSearchScope.allScope(project);
        var projectScope = GlobalSearchScope.projectScope(project);

        var jobInterface = facade.findClass(JOB, libraries);
        if (jobInterface == null) return List.of();

        var dbTriggers = QuartzDatabaseService.getInstance(project).getTriggers();
        Set<String> dbJobClasses = dbTriggers.stream()
                .map(trigger -> trigger.jobClassName().replace('$', '.'))
                .collect(Collectors.toSet());

        Set<PsiClass> jobs = new LinkedHashSet<>();
        for (var job : ClassInheritorsSearch.search(jobInterface, projectScope, true).findAll()) {
            if (!job.isInterface() && !job.hasModifierProperty(PsiModifier.ABSTRACT)) jobs.add(job);
        }

        List<JobNode> nodes = new ArrayList<>();
        Set<PsiClass> scheduledJobs = new HashSet<>();

        for (var setter : CRON_SETTERS) {
            var setterClass = facade.findClass(setter.className(), libraries);
            if (setterClass == null) continue;

            for (var method : setterClass.findMethodsByName(setter.methodName(), false)) {
                for (var reference : MethodReferencesSearch.search(method, projectScope, true).findAll()) {
                    if (!(reference.getElement().getParent() instanceof PsiMethodCallExpression call)) continue;

                    var cron = cronArgument(call);
                    if (cron == null) continue;

                    var job = findJob(call, jobs);
                    if (job != null) scheduledJobs.add(job);
                    if (job != null && dbJobClasses.contains(job.getQualifiedName())
                            && !CronResolver.resolve(cron, false).hasCron()) {
                        continue;
                    }
                    nodes.add(QuartzJobNode.ofTrigger(job, cron));
                }
            }
        }

        jobs.stream()
                .filter(job -> !scheduledJobs.contains(job) && !dbJobClasses.contains(job.getQualifiedName()))
                .forEach(job -> nodes.add(QuartzJobNode.ofJob(job)));

        for (var trigger : dbTriggers) {
            var job = facade.findClass(trigger.jobClassName().replace('$', '.'), projectScope);
            nodes.add(new QuartzDbJobNode(project, trigger, job));
        }
        return nodes;
    }

    /** The cron string, also when wrapped as {@code cronSchedule(new CronExpression("..."))}. */
    private static @Nullable PsiExpression cronArgument(PsiMethodCallExpression call) {
        var args = call.getArgumentList().getExpressions();
        if (args.length == 0) return null;

        if (args[0] instanceof PsiNewExpression newExpression
                && newExpression.getClassReference() != null
                && CRON_EXPRESSION.equals(newExpression.getClassReference().getQualifiedName())
                && newExpression.getArgumentList() != null
                && newExpression.getArgumentList().getExpressionCount() == 1) {
            return newExpression.getArgumentList().getExpressions()[0];
        }
        return args[0];
    }

    /**
     * Matches a trigger to its job, trying in turn:
     * <ol>
     *     <li>a job class literal next to the trigger, e.g. {@code newJob(ReportJob.class)} in the same method</li>
     *     <li>a job class literal in a method the trigger's method calls, e.g. {@code forJob(reportJobDetail())}</li>
     *     <li>a job class literal in the bean a parameter is named after, e.g.
     *     {@code reportTrigger(JobDetail reportJobDetail)} with a {@code reportJobDetail()} bean method</li>
     *     <li>either of the first two in the methods that call the trigger's method, e.g. a
     *     {@code createSchedule(cron)} helper called from {@code createJob}, which also calls
     *     {@code newJob(ReportJob.class)}; skipped if the callers schedule different jobs</li>
     * </ol>
     */
    private static @Nullable PsiClass findJob(PsiMethodCallExpression trigger, Set<PsiClass> jobs) {
        var method = PsiTreeUtil.getParentOfType(trigger, PsiMethod.class);
        if (method == null) return findJobLiteral(PsiTreeUtil.getParentOfType(trigger, PsiClass.class), jobs);

        var job = findJobNearby(method, jobs);
        if (job != null) return job;

        var containingClass = method.getContainingClass();
        if (containingClass == null) return null;
        for (var parameter : method.getParameterList().getParameters()) {
            for (var bean : containingClass.findMethodsByName(parameter.getName(), false)) {
                job = findJobLiteral(bean, jobs);
                if (job != null) return job;
            }
        }
        return findJobInCallers(method, jobs);
    }

    /** A job class literal in {@code method} or in a source method it calls. */
    private static @Nullable PsiClass findJobNearby(PsiMethod method, Set<PsiClass> jobs) {
        var job = findJobLiteral(method, jobs);
        if (job != null) return job;

        for (var call : PsiTreeUtil.findChildrenOfType(method, PsiMethodCallExpression.class)) {
            var target = call.resolveMethod();
            if (target == null || target == method || target instanceof PsiCompiledElement) continue;
            job = findJobLiteral(target, jobs);
            if (job != null) return job;
        }
        return null;
    }

    /**
     * Walks up the callers of {@code method} level by level until some of them are near a job. A shared helper called
     * for several jobs belongs to none of them, so the job is only returned if all callers at that level agree.
     */
    private static @Nullable PsiClass findJobInCallers(PsiMethod method, Set<PsiClass> jobs) {
        Set<PsiMethod> visited = new HashSet<>(Set.of(method));
        List<PsiMethod> level = List.of(method);

        for (int depth = 0; depth < MAX_CALLER_DEPTH && !level.isEmpty(); depth++) {
            Set<PsiClass> found = new HashSet<>();
            List<PsiMethod> next = new ArrayList<>();
            for (var callee : level) {
                for (var reference : MethodReferencesSearch.search(callee, callee.getUseScope(), true)) {
                    var caller = PsiTreeUtil.getParentOfType(reference.getElement(), PsiMethod.class);
                    if (caller == null || !visited.add(caller)) continue;

                    var job = findJobNearby(caller, jobs);
                    if (job != null) found.add(job);
                    else next.add(caller);
                }
            }
            if (!found.isEmpty()) return found.size() == 1 ? found.iterator().next() : null;
            level = next;
        }
        return null;
    }

    private static @Nullable PsiClass findJobLiteral(@Nullable PsiElement scope, Set<PsiClass> jobs) {
        if (scope == null) return null;
        for (var literal : PsiTreeUtil.findChildrenOfType(scope, PsiClassObjectAccessExpression.class)) {
            if (literal.getOperand().getType() instanceof PsiClassType type
                    && type.resolve() instanceof PsiClass cls
                    && jobs.contains(cls)) {
                return cls;
            }
        }
        return null;
    }
}
