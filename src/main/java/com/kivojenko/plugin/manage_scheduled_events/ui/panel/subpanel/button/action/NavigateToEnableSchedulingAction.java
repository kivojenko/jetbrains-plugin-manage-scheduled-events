package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ReadAction;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.ScheduledEventsTree;
import org.jetbrains.annotations.NotNull;

public class NavigateToEnableSchedulingAction extends AnAction {
    public NavigateToEnableSchedulingAction() {
        super("Scheduling", "", AllIcons.Actions.MoveToWindow);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ReadAction.run(() -> {
            var enable = ScheduledEventsTree.getInstance(e).getEnableScheduling();
            if (enable.isEmpty()) {
                return;
            }
            enable.get().navigate(true);
        });
    }
}
