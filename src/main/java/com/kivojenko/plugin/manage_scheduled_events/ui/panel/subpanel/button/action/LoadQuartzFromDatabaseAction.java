package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button.action;

import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.kivojenko.plugin.manage_scheduled_events.service.QuartzDatabase;
import com.kivojenko.plugin.manage_scheduled_events.service.QuartzDatabaseService;
import com.kivojenko.plugin.manage_scheduled_events.service.QuartzTrigger;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.ScheduledEventsTree;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Reads the jobs and triggers Quartz stored in its {@code QRTZ_} tables, through a data source from the Database tool
 * window. Asks which data source when several have the tables.
 */
public class LoadQuartzFromDatabaseAction extends AnAction {

    public LoadQuartzFromDatabaseAction() {
        super("Load Quartz Triggers from Database",
                "Read jobs and crons from the QRTZ_ tables of a data source in the Database tool window",
                AllIcons.Nodes.DataTables);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getProject() != null && QuartzDatabase.getInstance(e.getProject()) != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) return;
        var database = QuartzDatabase.getInstance(project);
        if (database == null) return;

        var dataContext = e.getDataContext();
        ReadAction.nonBlocking(database::findSources)
                .finishOnUiThread(ModalityState.defaultModalityState(),
                        sources -> chooseSource(project, database, sources, dataContext))
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    private static void chooseSource(Project project, QuartzDatabase database, List<QuartzDatabase.Source> sources,
                                     DataContext dataContext) {
        if (sources.isEmpty()) {
            notify(project, NotificationType.WARNING, "No data source with Quartz tables (QRTZ_JOB_DETAILS, "
                    + "QRTZ_TRIGGERS, QRTZ_CRON_TRIGGERS) found. Add the application's database in the Database "
                    + "tool window and make sure the schema with them is introspected.");
            return;
        }
        if (sources.size() == 1) {
            load(project, database, sources.getFirst());
            return;
        }
        JBPopupFactory.getInstance()
                .createPopupChooserBuilder(sources)
                .setTitle("Quartz Data Source")
                .setItemChosenCallback(source -> load(project, database, source))
                .createPopup()
                .showInBestPositionFor(dataContext);
    }

    private static void load(Project project, QuartzDatabase database, QuartzDatabase.Source source) {
        new Task.Backgroundable(project, "Loading Quartz triggers from " + source, true) {
            private List<QuartzTrigger> triggers = List.of();

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    triggers = database.read(source);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }

            @Override
            public void onSuccess() {
                QuartzDatabaseService.getInstance(project).setTriggers(triggers);
                ScheduledEventsTree.getInstance(project).refreshList();
                LoadQuartzFromDatabaseAction.notify(project, NotificationType.INFORMATION,
                        "Loaded " + triggers.size() + " Quartz trigger(s) from " + source + ".");
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                LoadQuartzFromDatabaseAction.notify(project, NotificationType.ERROR,
                        "Couldn't read Quartz triggers from " + source + ": " + error.getMessage());
            }
        }.queue();
    }

    private static void notify(Project project, NotificationType type, String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Scheduled Events")
                .createNotification(message, type)
                .notify(project);
    }
}
