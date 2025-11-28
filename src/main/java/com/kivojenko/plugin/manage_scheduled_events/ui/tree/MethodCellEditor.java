package com.kivojenko.plugin.manage_scheduled_events.ui.tree;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiMethod;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.CellEditorPanel;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.model.node.MethodNode;

import javax.swing.*;
import javax.swing.tree.TreeCellEditor;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.EventObject;
import java.util.Objects;

public class MethodCellEditor extends AbstractCellEditor implements TreeCellEditor {
    private final CellEditorPanel panel = new CellEditorPanel();
    private MethodNode currentNode;

    @Override
    public boolean isCellEditable(EventObject e) {
        if (!(e instanceof MouseEvent me)) return false;
        var tree = (JTree) me.getSource();

        var path = tree.getPathForLocation(me.getX(), me.getY());
        if (path == null) return false;

        return path.getLastPathComponent() instanceof MethodNode;
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                boolean leaf, int row) {
        currentNode = (MethodNode) value;
        panel.setNode(currentNode);
        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        String newCron = panel.getText();
        currentNode.setCron(newCron);
        updatePsiCron(currentNode.getPsiMethod(), newCron);
        return currentNode;
    }

    private void updatePsiCron(PsiMethod method, String newCron) {
        var project = method.getProject();
        var factory = JavaPsiFacade.getInstance(project).getElementFactory();

        WriteCommandAction.runWriteCommandAction(project, () -> {
            var ann = Arrays.stream(method.getModifierList().getAnnotations())
                    .filter(a -> Objects.requireNonNull(a.getQualifiedName()).endsWith(".Scheduled"))
                    .findFirst();
            if (ann.isEmpty()) return;

            var value = ann.get().findAttributeValue("cron");
            if (value == null) return;

            var cron = "\"" + newCron + "\"";
            var element = factory.createExpressionFromText(cron, value);
            value.replace(element);
        });
    }
}
