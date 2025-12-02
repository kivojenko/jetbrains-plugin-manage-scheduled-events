package com.kivojenko.plugin.manage_scheduled_events.ui.tree.node;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.*;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.ScheduledEventsTree;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

@Getter
@Setter
public class MethodNode extends TypedNode {
    private static CronDescriptor descriptor = CronDescriptor.instance(Locale.UK);
    private static CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING);
    private static CronParser parser = new CronParser(definition);

    private static final String SHEDLOCK = "net.javacrumbs.shedlock.spring.annotation.SchedulerLock";

    private final PsiMethod method;
    private final PsiAnnotation annotation;
    private final PsiAnnotation shedLockAnnotation;
    private final boolean isShedLockEnabled;

    public static MethodNode createFromAnnotation(PsiAnnotation annotation) {
        if (annotation == null || !(annotation.getOwner() instanceof PsiModifierList psiModifierList)) {
            throw new IllegalArgumentException("Annotation with PsiModifierList owner expected");
        }
        var method = psiModifierList.getParent();
        if (!(method instanceof PsiMethod psiMethod)) {
            throw new IllegalArgumentException("Method with PsiMethod owner expected");
        }

        PsiAnnotation shedLockAnnotation = null;
        boolean isShedLockEnabled = false;

        if (ScheduledEventsTree.getInstance(method.getProject()).getEnableShedLock().isPresent()) {
            shedLockAnnotation = Arrays.stream(psiModifierList.getAnnotations())
                    .filter(a -> SHEDLOCK.equals(a.getQualifiedName()))
                    .findFirst()
                    .orElse(null);
            isShedLockEnabled = true;
        }

        return new MethodNode(psiMethod, annotation, shedLockAnnotation, isShedLockEnabled);
    }

    public MethodNode(PsiMethod method, PsiAnnotation annotation, PsiAnnotation shedLockAnnotation,
                      boolean isShedLockEnabled) {
        super(method.getName(), NodeType.METHOD);
        this.method = method;
        this.annotation = annotation;
        this.shedLockAnnotation = shedLockAnnotation;
        this.isShedLockEnabled = isShedLockEnabled;
    }

    public String getCron() {
        return ReadAction.compute(() -> {
            var cron = annotation.findAttributeValue("cron");
            if (cron instanceof PsiLiteralExpression literal) {
                return (String) literal.getValue();
            }
            return "<no cron>";
        });
    }

    public void setCron(String newCron) {
        if (Objects.equals(newCron, getCron())) return;
        updatePsiCron(newCron);
    }

    private void updatePsiCron(String newCron) {
        var project = method.getProject();

        WriteCommandAction.runWriteCommandAction(project, () -> {
            var value = annotation.findAttributeValue("cron");

            if (value == null) return;
            var cron = "\"" + newCron + "\"";

            var element = JavaPsiFacade.getElementFactory(project).createExpressionFromText(cron, value);
            value.replace(element);
        });
    }

    public boolean isValid() {
        try {
            parser.parse(getCron());
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public String getCronDescription() {
        try {
            return descriptor.describe(parser.parse(getCron()));
        } catch (IllegalArgumentException e) {
            return e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        }
    }

    public String getFilePath() {
        return ((PsiJavaFile) method.getContainingFile()).getPackageName();
    }

    public String getClassName() {
        return ReadAction.compute(() -> method.getContainingClass() != null ? method.getContainingClass().getName() : "");
    }

    public String getName() {
        return method.getName();
    }
}
