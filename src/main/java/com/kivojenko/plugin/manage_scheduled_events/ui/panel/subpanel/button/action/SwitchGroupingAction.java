package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.Grouping;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.ScheduledEventsTree;
import org.jetbrains.annotations.NotNull;

/** Cycles through {@link Grouping} modes; the icon shows the current mode, the tooltip names the next one. */
public class SwitchGroupingAction extends AnAction {

    public SwitchGroupingAction() {
        super("Toggle Grouping", "Switch grouping mode", null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var tree = ScheduledEventsTree.getInstance(e);
        tree.setGrouping(tree.getGrouping().next());
        updatePresentation(e.getPresentation(), tree.getGrouping());
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        updatePresentation(e.getPresentation(), ScheduledEventsTree.getInstance(e).getGrouping());
    }

    public static void updatePresentation(Presentation presentation, Grouping grouping) {
        presentation.setIcon(grouping.getIcon());
        presentation.setText("Grouping: " + grouping.getLabel() + " (click for " + grouping.next().getLabel().toLowerCase() + ")");
    }
}
