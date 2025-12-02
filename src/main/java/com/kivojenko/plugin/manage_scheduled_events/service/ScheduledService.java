package com.kivojenko.plugin.manage_scheduled_events.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.impl.java.stubs.index.JavaAnnotationIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.MethodNode;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service(Service.Level.PROJECT)
public record ScheduledService(Project project) {

    @SneakyThrows
    public Optional<PsiClass> findEnableScheduling() {
        return ApplicationManager.getApplication()
                .executeOnPooledThread(() -> ReadAction.compute(() -> JavaAnnotationIndex
                        .getInstance()
                        .getAnnotations("EnableScheduling", project, GlobalSearchScope.projectScope(project))
                        .stream()
                        .map(ann -> (PsiModifierList) ann.getOwner())
                        .filter(Objects::nonNull)
                        .filter(owner -> owner.getParent() instanceof PsiClass)
                        .map(owner -> (PsiClass) owner.getParent())
                        .findAny()
                )).get();
    }

    @SneakyThrows
    public Optional<PsiClass> findEnableShedLock() {
        return ApplicationManager.getApplication()
                .executeOnPooledThread(() -> ReadAction.compute(() -> JavaAnnotationIndex
                        .getInstance()
                        .getAnnotations("EnableSchedulerLock", project, GlobalSearchScope.projectScope(project))
                        .stream()
                        .map(ann -> (PsiModifierList) ann.getOwner())
                        .filter(Objects::nonNull)
                        .filter(owner -> owner.getParent() instanceof PsiClass)
                        .map(owner -> (PsiClass) owner.getParent())
                        .findAny()
                )).get();
    }

    @SneakyThrows
    public List<MethodNode> findScheduledMethods() {
        return ApplicationManager.getApplication()
                .executeOnPooledThread(() -> ReadAction.compute(() -> JavaAnnotationIndex
                        .getInstance()
                        .getAnnotations("Scheduled", project, GlobalSearchScope.projectScope(project))
                        .stream()
                        .filter(ann -> ann.getOwner() instanceof PsiModifierList)
                        .filter(ann -> ((PsiModifierList) ann.getOwner()).getParent() instanceof PsiMethod)
                        .map(MethodNode::createFromAnnotation)
                        .toList())
                ).get();
    }
}
