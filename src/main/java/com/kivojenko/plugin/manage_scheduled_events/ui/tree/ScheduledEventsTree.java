package com.kivojenko.plugin.manage_scheduled_events.ui.tree;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.treeStructure.SimpleTree;
import com.kivojenko.plugin.manage_scheduled_events.service.ScheduledService;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.model.ScheduledMethod;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.model.node.MethodNode;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.model.node.tree.ComplexScheduledTreeNode;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.model.node.tree.SimpleScheduledTreeNode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultTreeModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@Service(Service.Level.PROJECT)
public final class ScheduledEventsTree extends SimpleTree {
    private final ScheduledService scheduledService;

    private Optional<PsiClass> enableScheduling = Optional.empty();
    private List<ScheduledMethod> methods = new ArrayList<>();
    private boolean groupByPackage = true;

    public ScheduledEventsTree(Project project) {
        super();
        this.scheduledService = project.getService(ScheduledService.class);

        setEditable(true);
        setInvokesStopCellEditing(true);
        setRootVisible(false);
        setShowsRootHandles(true);

        setCellRenderer(new MethodCellRenderer());
        setCellEditor(new MethodCellEditor());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    navigateOnDoubleClick(e);
                }
            }
        });

    }


    private void navigateOnDoubleClick(MouseEvent e) {
        var path = getPathForLocation(e.getX(), e.getY());
        if (path == null) return;

        var nodeObj = path.getLastPathComponent();
        if (!(nodeObj instanceof MethodNode node)) return;

        PsiMethod method = node.getPsiMethod();
        if (method == null || !method.isValid() || !method.canNavigate()) return;

        method.navigate(true);
    }

    public void refreshList() {
        setEnableScheduling(scheduledService.findEnableScheduling());
        setMethods(scheduledService.findScheduledMethods());
    }

    public void setMethods(List<ScheduledMethod> methods) {
        this.methods = methods;
        refreshTreeModel();
    }

    public void setGroupByPackage(boolean groupByPackage) {
        this.groupByPackage = groupByPackage;
        refreshTreeModel();
    }

    public void refreshTreeModel() {
        var node = groupByPackage ? new ComplexScheduledTreeNode(methods) : new SimpleScheduledTreeNode(methods);
        setModel(new DefaultTreeModel(node));
        expandAll();
    }

    public void expandAll() {
        for (int i = 0; i < getRowCount(); i++) expandRow(i);
    }

    public void collapseAll() {
        for (int i = getRowCount() - 1; i >= 0; i--) collapseRow(i);
    }

    public static ScheduledEventsTree getInstance(@Nullable AnActionEvent event) {
        if (event == null || event.getProject() == null) {
            return ProjectManager.getInstance().getOpenProjects()[0].getService(ScheduledEventsTree.class);
        }

        return event.getProject().getService(ScheduledEventsTree.class);
    }
}
