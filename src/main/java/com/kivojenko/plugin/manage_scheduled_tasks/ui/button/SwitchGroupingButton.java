package com.kivojenko.plugin.manage_scheduled_tasks.ui.button;

import com.kivojenko.plugin.manage_scheduled_tasks.ui.action.SwitchGroupingAction;

public class SwitchGroupingButton extends ScheduledButton {
    public SwitchGroupingButton() {
        super(new SwitchGroupingAction());
    }
}
