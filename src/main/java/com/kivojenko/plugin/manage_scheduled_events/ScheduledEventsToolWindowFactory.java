package com.kivojenko.plugin.manage_scheduled_events;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.ScheduledEventPanel;
import org.jetbrains.annotations.NotNull;

public class ScheduledEventsToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, ToolWindow toolWindow) {
        var panel = new ScheduledEventPanel(project);

        var factory = ContentFactory.getInstance();
        var content = factory.createContent(panel, "Scheduled Events", false);

        toolWindow.getContentManager().addContent(content);
    }
}
