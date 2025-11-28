package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button;

import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action.ExpandAllAction;

public class ExpandAllButton extends ScheduledButton<ExpandAllAction> {
    public ExpandAllButton() {
        super(new ExpandAllAction());
    }
}
