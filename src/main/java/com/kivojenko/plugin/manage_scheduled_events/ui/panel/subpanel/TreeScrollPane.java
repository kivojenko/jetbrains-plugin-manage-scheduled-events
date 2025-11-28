package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.ScheduledEventsTree;

public class TreeScrollPane extends JBScrollPane {
    public TreeScrollPane(Project project) {
        super(project.getService(ScheduledEventsTree.class));
    }
}
