package com.kivojenko.plugin.manage_scheduled_tasks.model;

import com.intellij.psi.PsiMethod;

public record ScheduledMethod(
        String filePath,
        String className,
        String methodName,
        String cron,
        PsiMethod psiMethod
) {

}
