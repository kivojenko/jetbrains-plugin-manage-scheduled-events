package com.kivojenko.plugin.manage_scheduled_events.ui.panel;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.FooterToolPanel;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.NavToolPanel;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.TreeScrollPane;

import java.awt.*;

public class ScheduledEventPanel extends JBPanel<ScheduledEventPanel> {
    public ScheduledEventPanel(Project project) {
        super(new BorderLayout());

        add(new NavToolPanel(project), BorderLayout.NORTH);
        add(new TreeScrollPane(project), BorderLayout.CENTER);
        add(new FooterToolPanel(project), BorderLayout.SOUTH);
    }
}
