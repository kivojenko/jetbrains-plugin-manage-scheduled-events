package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button;

import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action.SwitchSortingAction;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.Sorting;

public class SwitchSortingButton extends ScheduledButton<SwitchSortingAction> {
    public SwitchSortingButton() {
        this(new SwitchSortingAction());
    }

    public SwitchSortingButton(SwitchSortingAction action) {
        super(action);
        SwitchSortingAction.updatePresentation(myPresentation, Sorting.NAME);
    }
}
