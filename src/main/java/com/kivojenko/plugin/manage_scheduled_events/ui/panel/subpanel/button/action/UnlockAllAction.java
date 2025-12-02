package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.ScheduledEventsTree;
import org.jetbrains.annotations.NotNull;

public class UnlockAllAction extends AnAction {
    public UnlockAllAction() {
        super("Unlock All", "Unlock all methods", AllIcons.Ide.Readwrite);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ScheduledEventsTree.getInstance(e).collapseAll();
    }
}
