package com.kivojenko.plugin.manage_scheduled_tasks;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class ScheduledTasksToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, ToolWindow toolWindow) {
        ScheduledListPanel panel = new ScheduledListPanel(project);
        ContentFactory factory = ContentFactory.getInstance();
        Content content = factory.createContent(panel, "Scheduled Tool Window", false);
        toolWindow.getContentManager().addContent(content);
    }
}
