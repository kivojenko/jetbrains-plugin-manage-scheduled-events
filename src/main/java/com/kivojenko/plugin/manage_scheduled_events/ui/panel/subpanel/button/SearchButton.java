package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button;

import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action.SearchAction;

public class SearchButton extends ScheduledButton<SearchAction> {
    public SearchButton() {
        super(new SearchAction());
    }
}
