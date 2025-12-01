package com.kivojenko.plugin.manage_scheduled_events.ui.tree.node;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Locale;
import java.util.Objects;

@Getter
@Setter
public class MethodNode extends TypedNode {
    private static CronDescriptor descriptor = CronDescriptor.instance(Locale.UK);
    private static CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING);
    private static CronParser parser = new CronParser(definition);

    private final PsiMethod method;
    private final PsiAnnotation annotation;

    public static MethodNode createFromAnnotation(PsiAnnotation annotation) {
        if (annotation == null || !(annotation.getOwner() instanceof PsiModifierList psiModifierList)) {
            throw new IllegalArgumentException("Annotation with PsiModifierList owner expected");
        }
        var method = psiModifierList.getParent();
        if (!(method instanceof PsiMethod psiMethod)) {
            throw new IllegalArgumentException("Method with PsiMethod owner expected");
        }
        return new MethodNode(psiMethod, annotation);
    }

    public MethodNode(PsiMethod method, PsiAnnotation annotation) {
        super(method.getName(), NodeType.METHOD);
        this.method = method;
        this.annotation = annotation;
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
            return e.getCause().getMessage();
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
