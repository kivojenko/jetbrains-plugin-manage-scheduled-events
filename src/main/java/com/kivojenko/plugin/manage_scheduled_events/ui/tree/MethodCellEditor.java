package com.kivojenko.plugin.manage_scheduled_events.ui.tree;

import com.kivojenko.plugin.manage_scheduled_events.ui.panel.cell.CellEditorPanel;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.MethodNode;

import javax.swing.*;
import javax.swing.tree.TreeCellEditor;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.EventObject;

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
    public Component getTreeCellEditorComponent(JTree t, Object value, boolean selected, boolean e, boolean l, int r) {
        currentNode = (MethodNode) value;
        panel.setNode(currentNode);
        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        String newCron = panel.getText();
        currentNode.setCron(newCron);
        return currentNode;
    }
}
