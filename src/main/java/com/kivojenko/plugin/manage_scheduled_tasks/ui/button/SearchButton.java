package com.kivojenko.plugin.manage_scheduled_tasks.ui.button;

import com.kivojenko.plugin.manage_scheduled_tasks.ui.action.SearchAction;

public class SearchButton extends ScheduledButton {
    public SearchButton() {
        super(new SearchAction());
    }
}
