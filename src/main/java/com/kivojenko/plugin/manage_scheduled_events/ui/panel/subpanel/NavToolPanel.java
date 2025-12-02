package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.*;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action.LockAllAction;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action.UnlockAllAction;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.ScheduledEventsTree;

import java.awt.*;

public class NavToolPanel extends JBPanel<NavToolPanel> {
    public NavToolPanel(Project project) {
        super(new BorderLayout(8, 0));

        JBPanel<?> leftPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.add(new SearchButton());
        leftPanel.add(new ExpandAllButton());
        leftPanel.add(new CollapseAllButton());

        add(leftPanel, BorderLayout.WEST);

        DumbService.getInstance(project).runWhenSmart(() -> {
            var service = project.getService(ScheduledEventsTree.class);
            service.refreshList();

            var switchButton = new SwitchGroupingButton();
            leftPanel.add(switchButton);

            JBPanel<?> rightPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, 8, 0));

            rightPanel.add(new LockAllButton(new LockAllAction(), service));
            rightPanel.add(new UnlockAllButton(new UnlockAllAction(), service));

            add(rightPanel, BorderLayout.EAST);

        });
    }
}
