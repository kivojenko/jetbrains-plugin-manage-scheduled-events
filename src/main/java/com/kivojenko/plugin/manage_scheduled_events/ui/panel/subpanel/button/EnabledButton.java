package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action.NavigateToEnabledAction;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.ScheduledEventsTree;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class EnabledButton extends ActionButtonWithText {
    private final ScheduledEventsTree scheduledEventsTree;

    public EnabledButton(@NotNull NavigateToEnabledAction action, ScheduledEventsTree scheduledEventsTree) {
        super(action, action.getTemplatePresentation().clone(), ActionPlaces.UNKNOWN, getDimension());
        this.scheduledEventsTree = scheduledEventsTree;
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent event) {
        super.actionPerformed(event);

        var service = ScheduledEventsTree.getInstance(event);
        service.refreshList();

    }

    @Override
    public boolean isEnabled() {
        return scheduledEventsTree.getEnableScheduling().isPresent();
    }

    public static Dimension getDimension() {
        return new Dimension(30, 28);
    }
}
