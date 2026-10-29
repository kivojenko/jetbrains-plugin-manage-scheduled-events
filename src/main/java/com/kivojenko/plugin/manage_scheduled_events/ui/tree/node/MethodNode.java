package com.kivojenko.plugin.manage_scheduled_events.ui.tree.node;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.ScheduledEventsTree;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

/** A Spring {@code @Scheduled} method. */
@Getter
@Setter
public class MethodNode extends JobNode {
    private static final CronParser parser =
            new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING));

    public static final String SHEDLOCK ="net.javacrumbs.shedlock.spring.annotation.SchedulerLock";

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
        super(method.getName());
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

    @Override
    public Project getProject() {
        return methodPointer.getProject();
    }

    @Override
    public PsiElement getNavigationTarget() {
        return getMethod();
    }

    @Override
    protected CronParser getParser() {
        return parser;
    }

    /** Declared only, so {@code fixedRate} tasks show no cron rather than the attribute's empty default. */
    @Override
    protected PsiElement getCronElement() {
        return ReadAction.compute(() -> {
            var annotation = annotationPointer.getElement();
            return annotation == null ? null : annotation.findDeclaredAttributeValue("cron");
        });
    }

    @Override
    protected boolean resolvesPlaceholders() {
        return true;
    }

    @Override
    public String getFilePath() {
        return ReadAction.compute(() -> {
            var method = methodPointer.getElement();
            return method != null && method.getContainingFile() instanceof PsiJavaFile file ? file.getPackageName() : "";
        });
    }

    @Override
    public String getClassName() {
        return ReadAction.compute(() -> {
            var method = methodPointer.getElement();
            var cls = method == null ? null : method.getContainingClass();
            return cls != null && cls.getName() != null ? cls.getName() : "";
        });
    }

    @Override
    public String getName() {
        return name;
    }
}
