package com.kivojenko.plugin.manage_scheduled_events.ui.panel;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.model.node.MethodNode;

import javax.swing.*;
import java.awt.*;

public class CellEditorPanel extends JBPanel<CellEditorPanel> {
    private final JBLabel nameLabel = new JBLabel();
    private final JBLabel descriptionLabel = new JBLabel();
    private final JBTextField cronField = new JBTextField();

    public CellEditorPanel() {
        super(new BorderLayout(5, 0));

        nameLabel.setIcon(AllIcons.Nodes.Method);
        descriptionLabel.setForeground(JBColor.GRAY);

        add(nameLabel, BorderLayout.WEST);
        add(descriptionLabel, BorderLayout.CENTER);
        add(cronField, BorderLayout.EAST);

        setBackground(UIManager.getColor("Tree.selectionBackground"));
    }

    public void setNode(MethodNode node) {
        nameLabel.setText(node.getMethodName());
        descriptionLabel.setText(node.getCronDescription());
        cronField.setText(node.getCron());
    }

    public String getText() {
        return cronField.getText();
    }
}
