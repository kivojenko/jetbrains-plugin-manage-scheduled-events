package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.ScheduledEventsTree;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.Sorting;
import org.jetbrains.annotations.NotNull;

/** Cycles through {@link Sorting} modes; the icon shows the current mode, the tooltip names the next one. */
public class SwitchSortingAction extends AnAction {

    public SwitchSortingAction() {
        super("Toggle Sorting", "Switch sorting mode", null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var tree = ScheduledEventsTree.getInstance(e);
        tree.setSorting(tree.getSorting().next());
        updatePresentation(e.getPresentation(), tree.getSorting());
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        updatePresentation(e.getPresentation(), ScheduledEventsTree.getInstance(e).getSorting());
    }

    public static void updatePresentation(Presentation presentation, Sorting sorting) {
        presentation.setIcon(sorting.getIcon());
        presentation.setText("Sorting: " + sorting.getLabel() + " (click for " + sorting.next().getLabel().toLowerCase() + ")");
    }
}
