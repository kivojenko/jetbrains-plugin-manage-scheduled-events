package com.kivojenko.plugin.manage_scheduled_tasks.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.ui.JBColor;
import com.kivojenko.plugin.manage_scheduled_tasks.model.node.MethodNode;

import javax.swing.*;
import javax.swing.tree.TreeCellEditor;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.EventObject;
import java.util.Objects;


public class MethodCellEditor extends AbstractCellEditor implements TreeCellEditor {
    private final JPanel panel = new JPanel(new BorderLayout(5, 0));

    private final JLabel nameLabel = new JLabel();
    private final JLabel descriptionLabel = new JLabel();
    private final JTextField cronField = new JTextField();

    private MethodNode currentNode;
    private final Project project;
    private final PsiElementFactory factory;

    public MethodCellEditor(Project project) {
        this.project = project;
        factory = JavaPsiFacade.getElementFactory(project);

        nameLabel.setIcon(AllIcons.Nodes.Method);
        descriptionLabel.setForeground(JBColor.GRAY);

        panel.add(nameLabel, BorderLayout.WEST);
        panel.add(descriptionLabel, BorderLayout.CENTER);
        panel.add(cronField, BorderLayout.EAST);

        panel.setBackground(UIManager.getColor("Tree.selectionBackground"));
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        if (!(e instanceof MouseEvent me)) return false;
        var tree = (JTree) me.getSource();

        var path = tree.getPathForLocation(me.getX(), me.getY());
        if (path == null) return false;

        return path.getLastPathComponent() instanceof MethodNode;
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row) {
        currentNode = (MethodNode) value;

        nameLabel.setText(currentNode.getMethodName());
        descriptionLabel.setText(currentNode.getCronDescription());
        cronField.setText(currentNode.getCron());

        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        String newCron = cronField.getText();
        currentNode.setCron(newCron);
        updatePsiCron(currentNode.getPsiMethod(), newCron);
        return currentNode;
    }

    private void updatePsiCron(PsiMethod method, String newCron) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            var ann = Arrays.stream(method.getModifierList().getAnnotations())
                    .filter(a -> Objects.equals(a.getQualifiedName(), "org.springframework.scheduling.annotation.Scheduled"))
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
