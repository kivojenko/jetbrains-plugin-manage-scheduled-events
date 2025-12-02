package com.kivojenko.plugin.manage_scheduled_events.ui.panel.cell;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.IconUtil;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.MethodNode;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.TypedNode;

import javax.swing.*;
import java.awt.*;

public class CellRendererPanel extends JBPanel<CellRendererPanel> {
    private final JBLabel nameLabel = new JBLabel();
    private final JBLabel descriptionLabel = new JBLabel();
    private final JBLabel cronLabel = new JBLabel();
    private final JBLabel iconLabel = new JBLabel();

    public CellRendererPanel() {
        super(new BorderLayout(5, 0));
        add(nameLabel, BorderLayout.WEST);

        descriptionLabel.setForeground(JBColor.GRAY);

        JBPanel<?> rightPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.add(descriptionLabel);
        rightPanel.add(cronLabel);
        rightPanel.add(iconLabel);
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
            if (methodNode.isShedLockEnabled()) {
                Icon icon;
                Color color;

                if (methodNode.getShedLockAnnotation() == null) {
                    icon = AllIcons.Ide.Readwrite;
                    color = JBColor.RED;
                } else {
                    icon = AllIcons.Ide.Readonly;
                    color = JBColor.GRAY;
                }
                iconLabel.setIcon(IconUtil.colorize(icon, color));

            }

            descriptionLabel.setText(methodNode.getCronDescription());
            descriptionLabel.setForeground(methodNode.isValid() ? JBColor.GRAY : JBColor.RED);

            cronLabel.setText(String.format("%16s", methodNode.getCron()));

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
