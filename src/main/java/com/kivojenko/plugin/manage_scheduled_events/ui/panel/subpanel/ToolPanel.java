package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.*;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action.NavigateToEnabledAction;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action.SwitchGroupingAction;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.ScheduledEventsTree;

import java.awt.*;
import java.util.function.Supplier;

public class ToolPanel extends JBPanel<ToolPanel> {
    public ToolPanel(Project project) {
        super(new BorderLayout(5, 0));

        JBPanel<?> leftPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.add(new SearchButton());
        leftPanel.add(new ExpandAllButton());
        leftPanel.add(new CollapseAllButton());

        add(leftPanel, BorderLayout.WEST);

        DumbService.getInstance(project).runWhenSmart(() -> {
            var service = project.getService(ScheduledEventsTree.class);
            service.refreshList();

            var switchButton = new SwitchGroupingButton();
            ((SwitchGroupingAction) switchButton.getAction()).setSelected(null, true);
            leftPanel.add(switchButton, BorderLayout.CENTER);

            Supplier<String> text = () -> service.getEnableScheduling()
                    .map(psiClass -> "Scheduling enabled - " + psiClass.getName())
                    .orElse("Scheduling disabled");

            var action = new NavigateToEnabledAction(text);
            var button = new EnabledButton(action, service);
            add(button, BorderLayout.EAST);
        });

    }
}
