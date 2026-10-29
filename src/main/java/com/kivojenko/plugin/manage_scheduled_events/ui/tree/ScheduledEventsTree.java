package com.kivojenko.plugin.manage_scheduled_events.ui.tree;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.PsiNavigateUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.kivojenko.plugin.manage_scheduled_events.service.QuartzService;
import com.kivojenko.plugin.manage_scheduled_events.service.ScheduledService;
import com.kivojenko.plugin.manage_scheduled_events.service.ShedLockService;
import com.kivojenko.plugin.manage_scheduled_events.ui.panel.cell.CellRendererPanel;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.JobNode;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.MethodNode;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.tree.ComplexScheduledTreeNode;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.tree.FlatScheduledTreeNode;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.tree.SimpleScheduledTreeNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
@Service(Service.Level.PROJECT)
public final class ScheduledEventsTree extends SimpleTree implements Disposable {
    private final Project project;
    private final ScheduledService scheduledService;
    private final QuartzService quartzService;
    private final ShedLockService shedLockService;

    private Optional<PsiClass> enableScheduling = Optional.empty();
    private Optional<PsiClass> enableShedLock = Optional.empty();
    private List<JobNode> methods = new ArrayList<>();
    private Grouping grouping = Grouping.CLASS;
    private Sorting sorting = Sorting.NAME;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final List<Runnable> refreshListeners = new ArrayList<>();
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean viewportListenerAdded;
    /** Set when some description doesn't fit beside its name, so every row puts its description on its own lines. */
    @Setter(AccessLevel.NONE)
    private boolean descriptionsBelowNames;

    public ScheduledEventsTree(Project project) {
        super();
        this.project = project;
        scheduledService = project.getService(ScheduledService.class);
        quartzService = project.getService(QuartzService.class);
        shedLockService = project.getService(ShedLockService.class);

        setEditable(true);
        setInvokesStopCellEditing(true);
        setRootVisible(false);
        setShowsRootHandles(true);
        // Rows grow when their description wraps
        setRowHeight(0);

        // Short lines, as the tool window is often narrow and status text doesn't wrap
        getEmptyText().setText("No scheduled jobs found");
        getEmptyText().appendLine("Sources must be in a source root,", SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        getEmptyText().appendLine("e.g. an imported Gradle or Maven project", SimpleTextAttributes.GRAYED_ATTRIBUTES, null);

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

        // Code or config edits can change a cron, e.g. a value in application.yml
        project.getMessageBus().connect(this).subscribe(PsiModificationTracker.TOPIC, this::resolveCrons);
    }

    @Override
    public void dispose() {
    }

    /**
     * Resolves every job's cron in the background, as it needs indexes and config files, then repaints. Calls made
     * while one is running are merged into one.
     */
    public void resolveCrons() {
        var jobs = methods;
        ReadAction.nonBlocking(() -> {
                    jobs.forEach(JobNode::resolveCron);
                    return jobs;
                })
                .inSmartMode(project)
                .coalesceBy(this)
                .expireWith(this)
                .finishOnUiThread(ModalityState.defaultModalityState(), resolved -> {
                    if (resolved != methods) return;
                    // Frequency order depends on the crons; rebuilding would cancel an edit in progress
                    if (sorting == Sorting.FREQUENCY && !isEditing()) refreshTreeModel();
                    else relayoutRows();
                })
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    /** Rows are laid out to the visible width, so there is never anything to scroll to sideways. */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (viewportListenerAdded) return;

        var viewport = SwingUtilities.getAncestorOfClass(JViewport.class, this);
        if (viewport == null) return;
        viewport.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                relayoutRows();
            }
        });
        viewportListenerAdded = true;
    }

    /**
     * Re-decides where descriptions go for the current width and contents. Row heights depend on both, and the tree
     * caches them.
     */
    private void relayoutRows() {
        descriptionsBelowNames = false;
        if (getModel().getRoot() instanceof DefaultMutableTreeNode root) {
            for (var nodes = root.depthFirstEnumeration(); nodes.hasMoreElements() && !descriptionsBelowNames; ) {
                if (nodes.nextElement() instanceof JobNode node) {
                    var panel = new CellRendererPanel();
                    panel.setNode(node);
                    descriptionsBelowNames = !panel.fitsOnOneLine(MethodCellRenderer.availableWidth(this, node));
                }
            }
        }
        TreeUtil.invalidateCacheAndRepaint(getUI());
    }

    private void navigateOnDoubleClick(MouseEvent e) {
        var path = getPathForLocation(e.getX(), e.getY());
        if (path == null || !(path.getLastPathComponent() instanceof JobNode node)) return;

        var target = node.getNavigationTarget();
        if (target == null || !ReadAction.compute(target::isValid)) return;

        PsiNavigateUtil.navigate(target);
    }

    @SneakyThrows
    public void refreshList() {
        setEnableScheduling(scheduledService.findEnableScheduling());
        setEnableShedLock(scheduledService.findEnableShedLock());

        List<JobNode> jobs = new ArrayList<>(scheduledService.findScheduledMethods());
        jobs.addAll(quartzService.findQuartzJobs());
        setMethods(jobs);
        resolveCrons();

        refreshListeners.forEach(Runnable::run);
    }

    /** Runs after every {@link #refreshList()}, e.g. to show or hide UI depending on what was found. */
    public void addRefreshListener(Runnable listener) {
        refreshListeners.add(listener);
    }

    public boolean isShedLockFound() {
        return enableShedLock.isPresent();
    }

    /** ShedLock only applies to Spring {@code @Scheduled} methods. */
    private List<MethodNode> getSpringMethods() {
        return methods.stream().filter(MethodNode.class::isInstance).map(MethodNode.class::cast).toList();
    }

    public void setMethods(List<JobNode> methods) {
        this.methods = methods;
        refreshTreeModel();
    }

    public void setSorting(Sorting sorting) {
        this.sorting = sorting;
        refreshTreeModel();
    }

    public void setGrouping(Grouping grouping) {
        this.grouping = grouping;
        refreshTreeModel();
    }

    public void refreshTreeModel() {
        var sorted = sorting.sort(methods, grouping);
        DefaultMutableTreeNode node = switch (grouping) {
            case CLASS -> new SimpleScheduledTreeNode(sorted);
            case PACKAGE -> new ComplexScheduledTreeNode(sorted);
            case NONE -> new FlatScheduledTreeNode(sorted);
        };
        setModel(new DefaultTreeModel(node));
        expandAll();
        relayoutRows();
    }

    public void expandAll() {
        for (int i = 0; i < getRowCount(); i++) expandRow(i);
    }

    public void collapseAll() {
        for (int i = getRowCount() - 1; i >= 0; i--) collapseRow(i);
    }

    public void lockAll() {
        refreshList();
        shedLockService.lockAll(getSpringMethods());
    }

    public void unlockAll() {
        refreshList();
        shedLockService.unlockAll(getSpringMethods());
    }

    public static ScheduledEventsTree getInstance(@Nullable AnActionEvent event) {
        if (event == null || event.getProject() == null) {
            return ProjectManager.getInstance().getOpenProjects()[0].getService(ScheduledEventsTree.class);
        }
        return event.getProject().getService(ScheduledEventsTree.class);
    }

    public static ScheduledEventsTree getInstance(@Nullable Project project) {
        return Objects.requireNonNullElseGet(project, () -> ProjectManager.getInstance().getOpenProjects()[0])
                .getService(ScheduledEventsTree.class);

    }
}
