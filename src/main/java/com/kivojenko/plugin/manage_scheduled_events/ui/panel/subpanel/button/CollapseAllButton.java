package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button;

import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action.CollapseAllAction;

public class CollapseAllButton extends ScheduledButton<CollapseAllAction> {
    public CollapseAllButton() {
        super(new CollapseAllAction());
    }
}
