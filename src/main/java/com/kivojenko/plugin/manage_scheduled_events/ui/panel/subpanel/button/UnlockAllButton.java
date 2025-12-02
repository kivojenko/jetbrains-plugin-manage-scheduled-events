package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action.UnlockAllAction;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.ScheduledEventsTree;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class UnlockAllButton extends ActionButtonWithText {
    private final ScheduledEventsTree service;

    public UnlockAllButton(@NotNull UnlockAllAction action, ScheduledEventsTree service) {
        super(action, action.getTemplatePresentation().clone(), ActionPlaces.UNKNOWN, getDimension());
        this.service = service;
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent event) {
        super.actionPerformed(event);

        var service = ScheduledEventsTree.getInstance(event);
        service.unlockAll();
        service.refreshList();
    }

    @Override
    public boolean isEnabled() {
        return service.getEnableShedLock().isPresent();
    }

    public static Dimension getDimension() {
        return new Dimension(30, 28);
    }
}
