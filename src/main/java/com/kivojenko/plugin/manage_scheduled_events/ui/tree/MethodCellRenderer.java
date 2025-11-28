package com.kivojenko.plugin.manage_scheduled_events.ui.tree;

import com.kivojenko.plugin.manage_scheduled_events.ui.panel.CellRendererPanel;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.model.node.TypedNode;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class MethodCellRenderer implements TreeCellRenderer {
    private final CellRendererPanel panel = new CellRendererPanel();

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {

        if (value instanceof TypedNode node) {
            panel.setNode(node);
        } else {
            panel.setText(value.toString());
        }

        panel.setBackground(selected ? UIManager.getColor("Tree.selectionBackground") : tree.getBackground());

        return panel;
    }
}
