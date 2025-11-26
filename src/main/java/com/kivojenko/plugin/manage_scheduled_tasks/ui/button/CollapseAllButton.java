package com.kivojenko.plugin.manage_scheduled_tasks.ui.button;

import com.kivojenko.plugin.manage_scheduled_tasks.ui.action.CollapseAllAction;

public class CollapseAllButton extends ScheduledButton {
    public CollapseAllButton() {
        super(new CollapseAllAction());
    }
}
