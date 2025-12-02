package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.EnableSchedulingButton;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.EnableShedLockButton;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action.NavigateToEnableSchedulingAction;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action.NavigateToEnableShedLockAction;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.ScheduledEventsTree;

import java.awt.*;

public class FooterToolPanel extends JBPanel<FooterToolPanel> {
    public FooterToolPanel(Project project) {
        super(new FlowLayout(FlowLayout.LEFT, 8, 0));

        DumbService.getInstance(project).runWhenSmart(() -> {
            var service = project.getService(ScheduledEventsTree.class);
            if (service.getEnableScheduling().isEmpty()) service.refreshList();

            var enableSchedulingButton = new EnableSchedulingButton(new NavigateToEnableSchedulingAction(), service);
            add(enableSchedulingButton);

            var schedulerLockButton = new EnableShedLockButton(new NavigateToEnableShedLockAction(), service);
            add(schedulerLockButton);
        });

    }
}
