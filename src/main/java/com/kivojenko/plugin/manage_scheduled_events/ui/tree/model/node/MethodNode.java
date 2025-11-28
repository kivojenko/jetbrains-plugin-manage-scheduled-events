package com.kivojenko.plugin.manage_scheduled_events.ui.tree.model.node;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.intellij.psi.PsiMethod;
import lombok.Getter;
import lombok.Setter;

import java.util.Locale;

@Getter
@Setter
public class MethodNode extends TypedNode {
    private final PsiMethod psiMethod;
    private String cron;

    public MethodNode(String display, PsiMethod psiMethod, String cron) {
        super(display, NodeType.METHOD);
        this.psiMethod = psiMethod;
        this.cron = cron;
    }

    public String getMethodName() { return psiMethod.getName(); }

    public String getCronDescription() {
        var descriptor = CronDescriptor.instance(Locale.UK);
        var parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING));
        return descriptor.describe(parser.parse(cron));
    }
}
