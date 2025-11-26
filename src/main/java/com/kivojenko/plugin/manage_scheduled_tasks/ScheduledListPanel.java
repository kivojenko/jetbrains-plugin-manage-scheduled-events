package com.kivojenko.plugin.manage_scheduled_tasks;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.kivojenko.plugin.manage_scheduled_tasks.service.ListPanelService;
import com.kivojenko.plugin.manage_scheduled_tasks.ui.button.CollapseAllButton;
import com.kivojenko.plugin.manage_scheduled_tasks.ui.button.ExpandAllButton;
import com.kivojenko.plugin.manage_scheduled_tasks.ui.button.SearchButton;
import com.kivojenko.plugin.manage_scheduled_tasks.ui.button.SwitchGroupingButton;

import javax.swing.*;
import java.awt.*;

public class ScheduledListPanel extends JPanel {
    public ScheduledListPanel(Project project) {
        setLayout(new BorderLayout());

        add(createToolbar(), BorderLayout.NORTH);
        var tree = ListPanelService.getInstance(project).getTree();
        var pane = new JBScrollPane(tree);
        add(pane, BorderLayout.CENTER);
    }

    private JComponent createToolbar() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        panel.add(new SearchButton());
        panel.add(new ExpandAllButton());
        panel.add(new CollapseAllButton());
        panel.add(new SwitchGroupingButton());

        return panel;
    }

}
