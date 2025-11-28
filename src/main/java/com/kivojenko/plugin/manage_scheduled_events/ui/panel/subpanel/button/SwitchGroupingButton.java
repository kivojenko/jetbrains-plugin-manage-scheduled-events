package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button;

import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action.SwitchGroupingAction;

public class SwitchGroupingButton extends ScheduledButton<SwitchGroupingAction> {
    public SwitchGroupingButton() {
        super(new SwitchGroupingAction());
    }
}
