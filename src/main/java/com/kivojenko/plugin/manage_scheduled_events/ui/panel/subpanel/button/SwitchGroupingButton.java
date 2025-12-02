package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button;

import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action.SwitchGroupingAction;

public class SwitchGroupingButton extends ScheduledButton<SwitchGroupingAction> {
    public SwitchGroupingButton() {
        this(new SwitchGroupingAction());
    }

    public SwitchGroupingButton(SwitchGroupingAction action) {
        super(action);
        action.setSelected(null, false);
    }
}
