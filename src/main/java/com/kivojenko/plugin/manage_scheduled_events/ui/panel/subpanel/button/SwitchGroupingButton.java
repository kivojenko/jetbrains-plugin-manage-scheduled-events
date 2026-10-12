package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button;

import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action.SwitchGroupingAction;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.Grouping;

public class SwitchGroupingButton extends ScheduledButton<SwitchGroupingAction> {
    public SwitchGroupingButton() {
        this(new SwitchGroupingAction());
    }

    public SwitchGroupingButton(SwitchGroupingAction action) {
        super(action);
        SwitchGroupingAction.updatePresentation(myPresentation, Grouping.CLASS);
    }
}
