package com.kivojenko.plugin.manage_scheduled_tasks.ui.button;

import com.kivojenko.plugin.manage_scheduled_tasks.ui.action.ExpandAllAction;

public class ExpandAllButton extends ScheduledButton {
    public ExpandAllButton() {
        super(new ExpandAllAction());
    }
}
