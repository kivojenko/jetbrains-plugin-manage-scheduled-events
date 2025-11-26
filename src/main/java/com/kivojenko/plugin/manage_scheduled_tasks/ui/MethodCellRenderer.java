package com.kivojenko.plugin.manage_scheduled_tasks.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.kivojenko.plugin.manage_scheduled_tasks.model.node.MethodNode;
import com.kivojenko.plugin.manage_scheduled_tasks.model.node.TypedNode;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class MethodCellRenderer implements TreeCellRenderer {
    private final JPanel panel = new JPanel(new BorderLayout(5, 0));

    private final JLabel nameLabel = new JLabel();
    private final JLabel descriptionLabel = new JLabel();
    private final JLabel cronLabel = new JLabel();

    public MethodCellRenderer() {
        descriptionLabel.setForeground(JBColor.GRAY);
        cronLabel.setForeground(JBColor.GRAY);

        panel.add(nameLabel, BorderLayout.WEST);
        panel.add(descriptionLabel, BorderLayout.CENTER);
        panel.add(cronLabel, BorderLayout.EAST);
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree, Object value, boolean selected,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {

        if (value instanceof TypedNode node) {
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

                cronLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

            } else {
                nameLabel.setText(node.getUserObject().toString());
                descriptionLabel.setText("");
                cronLabel.setText("");
            }

        } else {
            nameLabel.setText(value.toString());
            nameLabel.setIcon(null);
        }

        panel.setBackground(selected ? UIManager.getColor("Tree.selectionBackground") : tree.getBackground());

        return panel;
    }
}
