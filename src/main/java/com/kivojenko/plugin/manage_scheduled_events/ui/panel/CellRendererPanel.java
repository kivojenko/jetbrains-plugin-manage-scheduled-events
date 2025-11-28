package com.kivojenko.plugin.manage_scheduled_events.ui.panel;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.model.node.MethodNode;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.model.node.TypedNode;

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
        add(descriptionLabel, BorderLayout.CENTER);
        add(cronLabel, BorderLayout.EAST);
    }

    public void setNode(TypedNode node) {
        Icon icon = switch (node.getType()) {
            case FOLDER -> AllIcons.Nodes.Folder;
            case PACKAGE -> AllIcons.Nodes.Package;
            case CLASS -> AllIcons.Nodes.Class;
            case METHOD -> AllIcons.Nodes.Method;
        };

        nameLabel.setIcon(icon);

        if (node instanceof MethodNode mn) {
            nameLabel.setText(mn.getMethodName());
            descriptionLabel.setText(mn.getCronDescription());
            cronLabel.setText(mn.getCron());
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
