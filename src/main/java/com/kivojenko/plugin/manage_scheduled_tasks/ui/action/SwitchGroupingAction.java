package com.kivojenko.plugin.manage_scheduled_tasks.ui.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.kivojenko.plugin.manage_scheduled_tasks.service.ListPanelService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.function.Supplier;

public class SwitchGroupingAction extends ToggleAction {

    public SwitchGroupingAction() {
        super("Toggle Grouping", "Switch grouping mode", null);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return ListPanelService.getInstance(e).isGroupByPackage();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        var service = ListPanelService.getInstance(e);
        service.setGroupByPackage(state);

        Supplier<Icon> iconSupplier = () -> state ? AllIcons.Actions.GroupByClass : AllIcons.Actions.GroupByPackage;
        getTemplatePresentation().setIconSupplier(iconSupplier);

        service.refreshList();
    }
}
