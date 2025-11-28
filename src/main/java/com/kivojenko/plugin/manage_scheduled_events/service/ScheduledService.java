package com.kivojenko.plugin.manage_scheduled_events.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.model.ScheduledMethod;

import java.util.*;

@Service(Service.Level.PROJECT)
public record ScheduledService(Project project) {
    public Optional<PsiClass> findEnableScheduling() {
        return JavaAnnotationIndex
                .getInstance()
                .getAnnotations("EnableScheduling", project, GlobalSearchScope.projectScope(project))
                .stream()
                .map(ann -> (PsiModifierList) ann.getOwner())
                .filter(Objects::nonNull)
                .filter(owner -> owner.getParent() instanceof PsiClass)
                .map(owner -> (PsiClass) owner.getParent())
                .findAny();
    }


    public List<ScheduledMethod> findScheduledMethods() {
        return JavaAnnotationIndex
                .getInstance()
                .getAnnotations("Scheduled", project, GlobalSearchScope.projectScope(project))
                .stream()
                .map(ann -> (PsiModifierList) ann.getOwner())
                .filter(Objects::nonNull)
                .filter(owner -> owner.getParent() instanceof PsiMethod)
                .map(owner -> (PsiMethod) owner.getParent())
                .filter(method -> method.getContainingClass() != null)
                .filter(method -> method.getContainingFile() instanceof PsiJavaFile)
                .map(method ->
                        new ScheduledMethod(
                                ((PsiJavaFile) method.getContainingFile()).getPackageName(),
                                method.getContainingClass().getName(),
                                method.getName(),
                                extractCron(method),
                                method
                        ))
                .toList();
    }

    private static String extractCron(PsiMethod method) {
        return Arrays.stream(method.getAnnotations())
                .flatMap(ann -> Arrays.stream(ann.getParameterList().getAttributes()))
                .filter(attr -> attr.getName() == null || attr.getName().equals("cron"))
                .filter(attr -> attr.getValue() != null && attr.getValue() instanceof PsiLiteralExpression)
                .map(attr -> (PsiLiteralExpression) attr.getValue())
                .filter(Objects::nonNull)
                .filter(expr -> expr.getValue() != null && expr.getValue() instanceof String)
                .map(expr -> (String) expr.getValue())
                .findFirst()
                .orElse("<no-cron>");
    }
}
