package com.kivojenko.plugin.manage_scheduled_events.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import java.util.List;

/**
 * The Quartz triggers last read from a database, kept so that refreshing the list shows them again without
 * reconnecting.
 */
@Service(Service.Level.PROJECT)
public final class QuartzDatabaseService {
    private volatile List<QuartzTrigger> triggers = List.of();

    public static QuartzDatabaseService getInstance(Project project) {
        return project.getService(QuartzDatabaseService.class);
    }

    public List<QuartzTrigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<QuartzTrigger> triggers) {
        this.triggers = List.copyOf(triggers);
    }
}
