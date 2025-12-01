package com.kivojenko.plugin.manage_scheduled_events.ui.panel;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.MethodNode;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.TypedNode;

import javax.swing.*;
import java.awt.*;

public class CellRendererPanel extends JBPanel<CellRendererPanel> {
    private final JBLabel nameLabel = new JBLabel();
    private final JBLabel descriptionLabel = new JBLabel();
    private final JBLabel cronLabel = new JBLabel();

    public CellRendererPanel() {
        super(new BorderLayout(5, 0));

        descriptionLabel.setForeground(JBColor.GRAY);
        cronLabel.setForeground(JBColor.GRAY);
        cronLabel.withBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));

        add(nameLabel, BorderLayout.WEST);

        JBPanel<?> rightPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.add(descriptionLabel);
        rightPanel.add(cronLabel);
        rightPanel.setOpaque(false);

        add(rightPanel, BorderLayout.CENTER);
    }

    public void setNode(TypedNode node) {
        nameLabel.setIcon(
                switch (node.getType()) {
                    case FOLDER -> AllIcons.Nodes.Folder;
                    case PACKAGE -> AllIcons.Nodes.Package;
                    case CLASS -> AllIcons.Nodes.Class;
                    case METHOD -> AllIcons.Nodes.Method;
                }
        );

        if (node instanceof MethodNode methodNode) {
            nameLabel.setText(methodNode.getName());
            descriptionLabel.setText(methodNode.getCronDescription());
            descriptionLabel.setForeground(methodNode.isValid() ? JBColor.BLUE : JBColor.RED);
            cronLabel.setText(methodNode.getCron());

        } else {
            nameLabel.setText(node.getUserObject().toString());
            descriptionLabel.setText("");
            cronLabel.setText("");
        }
    }

    public void setText(String text) {
        nameLabel.setText(text);
        nameLabel.setIcon(null);

        descriptionLabel.setText("");
        cronLabel.setText("");
    }
}
