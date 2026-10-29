package com.kivojenko.plugin.manage_scheduled_events.service;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Reads Quartz triggers from the {@code QRTZ_} tables of a data source in the Database tool window. Only registered when
 * the Database plugin is installed (IntelliJ IDEA Ultimate), see {@code with-database.xml}.
 */
public interface QuartzDatabase {

    /**
     * Where the Quartz tables are.
     *
     * @param id     the data source's unique id
     * @param prefix the table prefix as named in the database, e.g. {@code qrtz_}
     */
    record Source(String id, String name, @Nullable String schema, String prefix) {
        @Override
        public String toString() {
            return schema == null || schema.isBlank() ? name : name + " (" + schema + ")";
        }
    }

    /** {@code null} without the Database plugin. */
    static @Nullable QuartzDatabase getInstance(Project project) {
        return project.getService(QuartzDatabase.class);
    }

    /**
     * Data sources whose introspected schema has the Quartz tables. Doesn't connect, so a schema that hasn't been
     * introspected isn't found. Must run in a read action.
     */
    List<Source> findSources();

    /** Connects, asking for a password if needed, and reads every job with its triggers. Blocks; not on the UI thread. */
    List<QuartzTrigger> read(Source source) throws Exception;
}
