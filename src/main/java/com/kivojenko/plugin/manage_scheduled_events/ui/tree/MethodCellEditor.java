package com.kivojenko.plugin.manage_scheduled_events.ui.tree;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.cell.CellEditorPanel;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.JobNode;

import javax.swing.*;
import javax.swing.tree.TreeCellEditor;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.EventObject;

public class MethodCellEditor extends AbstractCellEditor implements TreeCellEditor {
    private final CellEditorPanel panel = new CellEditorPanel();
    private JobNode currentNode;

    @Override
    public boolean isCellEditable(EventObject e) {
        if (!(e instanceof MouseEvent me)) return false;
        var tree = (JTree) me.getSource();

        var path = tree.getPathForLocation(me.getX(), me.getY());
        if (path == null) return false;

        return path.getLastPathComponent() instanceof JobNode node && node.isEditable();
    }

    @Override
    public Component getTreeCellEditorComponent(JTree t, Object value, boolean selected, boolean e, boolean l, int r) {
        currentNode = (JobNode) value;
        panel.setNode(currentNode);
        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        var node = currentNode;
        node.setCron(panel.getText(), saved -> {
            if (!saved) notifyCronNotSaved(node);
        });
        return node;
    }

    private static void notifyCronNotSaved(JobNode node) {
        var project = node.getProject();
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Scheduled Events")
                .createNotification("Cron not saved for " + node.getClassName() + "." + node.getName()
                        + ": it comes from a shared constant, its property has no value in application.yml / "
                        + ".properties, or it was removed. List was refreshed.",
                        NotificationType.WARNING)
                .notify(project);
        // Defer so the tree finishes stopping the edit before its model is replaced.
        SwingUtilities.invokeLater(() -> ScheduledEventsTree.getInstance(project).refreshList());
    }
}
