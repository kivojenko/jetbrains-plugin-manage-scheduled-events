package com.kivojenko.plugin.manage_scheduled_tasks.service;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.treeStructure.SimpleTree;
import com.kivojenko.plugin.manage_scheduled_tasks.model.*;
import com.kivojenko.plugin.manage_scheduled_tasks.model.node.MethodNode;
import com.kivojenko.plugin.manage_scheduled_tasks.model.node.tree.ComplexScheduledTreeNode;
import com.kivojenko.plugin.manage_scheduled_tasks.model.node.tree.SimpleScheduledTreeNode;
import com.kivojenko.plugin.manage_scheduled_tasks.ui.MethodCellEditor;
import com.kivojenko.plugin.manage_scheduled_tasks.ui.MethodCellRenderer;
import lombok.Data;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

@Data
public class ListPanelService {
    private final Project project;

    private List<ScheduledMethod> methods = new ArrayList<>();
    private boolean groupByPackage = true;
    private final SimpleTree tree = new SimpleTree();

    public ListPanelService(Project project) {
        this.project = project;

        tree.setEditable(true);
        tree.setInvokesStopCellEditing(true);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    navigateOnDoubleClick(e);
                }
            }
        });

        tree.setCellRenderer(new MethodCellRenderer());
        tree.setCellEditor(new MethodCellEditor(project));
    }

    private void navigateOnDoubleClick(MouseEvent e) {
        var path = tree.getPathForLocation(e.getX(), e.getY());
        if (path == null) return;

        var nodeObj = path.getLastPathComponent();
        if (!(nodeObj instanceof MethodNode node)) return;

        PsiMethod method = node.getPsiMethod();
        if (method == null || !method.isValid() || !method.canNavigate()) return;

        method.navigate(true);
    }

    public void refreshList() {
        methods = ScheduledService.findScheduledMethods(project);
        refreshTreeModel();
        expandAll();
    }

    public void refreshTreeModel() {
        tree.setModel(new DefaultTreeModel(getNode()));
    }

    private DefaultMutableTreeNode getNode() {
        if (groupByPackage) {
            return new ComplexScheduledTreeNode(methods);
        }
        return new SimpleScheduledTreeNode(methods);
    }

    public void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    public void collapseAll() {
        for (int i = tree.getRowCount() - 1; i >= 0; i--) {
            tree.collapseRow(i);
        }
    }

    public static ListPanelService getInstance(Project project) {
        return project.getService(ListPanelService.class);
    }

    public static ListPanelService getInstance(AnActionEvent event) {
        if (event.getProject() == null) {
            throw new IllegalArgumentException("Project is null");
        }
        return getInstance(event.getProject());
    }

}
