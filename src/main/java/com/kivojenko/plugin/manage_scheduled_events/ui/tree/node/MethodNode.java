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

    private final SmartPsiElementPointer<PsiMethod> methodPointer;
    private final SmartPsiElementPointer<PsiAnnotation> annotationPointer;
    private final SmartPsiElementPointer<PsiAnnotation> shedLockPointer;
    private final boolean isShedLockEnabled;
    private final String name;

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
        var pointerManager = SmartPointerManager.getInstance(method.getProject());
        this.methodPointer = pointerManager.createSmartPsiElementPointer(method);
        this.annotationPointer = pointerManager.createSmartPsiElementPointer(annotation);
        this.shedLockPointer = shedLockAnnotation == null
                ? null
                : pointerManager.createSmartPsiElementPointer(shedLockAnnotation);
        this.isShedLockEnabled = isShedLockEnabled;
        this.name = method.getName();
    }

    /** PSI elements can be invalidated by edits or reparsing, so they are always resolved through pointers. */
    public PsiMethod getMethod() {
        return ReadAction.compute(methodPointer::getElement);
    }

    public PsiAnnotation getAnnotation() {
        return ReadAction.compute(annotationPointer::getElement);
    }

    public PsiAnnotation getShedLockAnnotation() {
        return shedLockPointer == null ? null : ReadAction.compute(shedLockPointer::getElement);
    }

    public String getCron() {
        return ReadAction.compute(() -> {
            var annotation = annotationPointer.getElement();
            if (annotation == null) return "<no cron>";
            var cron = annotation.findAttributeValue("cron");
            if (cron instanceof PsiLiteralExpression literal) {
                return (String) literal.getValue();
            }
            return "<no cron>";
        });
    }

    /**
     * @return {@code false} if the cron could not be written, e.g. the annotation was removed or has no
     * {@code cron} attribute since this node was created.
     */
    public boolean setCron(String newCron) {
        if (Objects.equals(newCron, getCron())) return true;
        return updatePsiCron(newCron);
    }

    private boolean updatePsiCron(String newCron) {
        var project = methodPointer.getProject();

        return WriteCommandAction.writeCommandAction(project).compute(() -> {
            var annotation = annotationPointer.getElement();
            if (annotation == null) return false;
            var value = annotation.findAttributeValue("cron");

            if (value == null) return false;
            var cron = "\"" + newCron + "\"";

            var element = JavaPsiFacade.getElementFactory(project).createExpressionFromText(cron, value);
            value.replace(element);
            return true;
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
        return ReadAction.compute(() -> {
            var method = methodPointer.getElement();
            return method != null && method.getContainingFile() instanceof PsiJavaFile file ? file.getPackageName() : "";
        });
    }

    public String getClassName() {
        return ReadAction.compute(() -> {
            var method = methodPointer.getElement();
            var cls = method == null ? null : method.getContainingClass();
            return cls != null && cls.getName() != null ? cls.getName() : "";
        });
    }

    public String getName() {
        return name;
    }
}
