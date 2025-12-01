package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.ScheduledEventsTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Supplier;

public class SwitchGroupingAction extends ToggleAction {

    public SwitchGroupingAction() {
        super("Toggle Grouping", "Switch grouping mode", null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return ScheduledEventsTree.getInstance(e).isGroupByPackage();
    }

    private static Supplier<Icon> getIconSupplier(@Nullable AnActionEvent e) {
        var state = ScheduledEventsTree.getInstance(e).isGroupByPackage();
        return () -> state ? AllIcons.Actions.GroupByClass : AllIcons.Actions.GroupByPackage;
    }

    @Override
    public void setSelected(@Nullable AnActionEvent e, boolean state) {
        ScheduledEventsTree.getInstance(e).setGroupByPackage(state);
        getTemplatePresentation().setIconSupplier(getIconSupplier(e));
    }
}
