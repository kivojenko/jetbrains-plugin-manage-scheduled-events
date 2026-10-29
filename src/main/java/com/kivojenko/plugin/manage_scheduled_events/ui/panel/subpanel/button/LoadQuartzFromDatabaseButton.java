package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button;

import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action.LoadQuartzFromDatabaseAction;

public class LoadQuartzFromDatabaseButton extends ScheduledButton<LoadQuartzFromDatabaseAction> {
    public LoadQuartzFromDatabaseButton() {
        super(new LoadQuartzFromDatabaseAction());
    }
}
