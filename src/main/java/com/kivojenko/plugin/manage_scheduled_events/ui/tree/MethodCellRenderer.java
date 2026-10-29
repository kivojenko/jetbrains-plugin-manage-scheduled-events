package com.kivojenko.plugin.manage_scheduled_events.ui.tree;

import com.intellij.util.ui.JBUI;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.cell.CellRendererPanel;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.TypedNode;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
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
        panel.fitTo(availableWidth(tree, value),
                tree instanceof ScheduledEventsTree scheduledTree && scheduledTree.isDescriptionsBelowNames());

        panel.setBackground(selected ? UIManager.getColor("Tree.selectionBackground") : tree.getBackground());
        return panel;
    }

    /**
     * Visible width left for a row after its indent, so the row fits without scrolling sideways. Worked out from the
     * node's depth, as asking the tree for the row's bounds would call this renderer again.
     */
    static int availableWidth(JTree tree, Object value) {
        var viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, tree);
        if (viewport == null || !(value instanceof DefaultMutableTreeNode node)) return 0;

        int indent = tree.getUI() instanceof BasicTreeUI ui
                ? ui.getLeftChildIndent() + ui.getRightChildIndent()
                : JBUI.scale(20);
        int levels = node.getLevel() - (tree.isRootVisible() ? 0 : 1) + (tree.getShowsRootHandles() ? 1 : 0);
        var insets = tree.getInsets();

        return viewport.getExtentSize().width - levels * indent - insets.left - insets.right - JBUI.scale(4);
    }
}
