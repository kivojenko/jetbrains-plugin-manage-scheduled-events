package com.kivojenko.plugin.manage_scheduled_events.ui.tree;

import com.kivojenko.plugin.manage_scheduled_events.ui.panel.CellRendererPanel;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.TypedNode;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class MethodCellRenderer implements TreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean e, boolean l,
                                                  int r, boolean h) {
        var panel = new CellRendererPanel();
        if (value instanceof TypedNode node) {
            panel.setNode(node);
        } else {
            panel.setText(value.toString());
        }

        panel.setBackground(selected ? UIManager.getColor("Tree.selectionBackground") : tree.getBackground());
        return panel;
    }
}
