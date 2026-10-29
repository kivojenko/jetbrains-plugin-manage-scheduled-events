package com.kivojenko.plugin.manage_scheduled_events.service.database;

import com.intellij.database.dataSource.DatabaseConnectionManager;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.dataSource.connection.ConnectionRequestor;
import com.intellij.database.model.DasObject;
import com.intellij.database.psi.DbPsiFacade;
import com.intellij.database.remote.jdbc.RemoteResultSet;
import com.intellij.database.util.DasUtil;
import com.intellij.openapi.project.Project;
import com.kivojenko.plugin.manage_scheduled_events.service.QuartzDatabase;
import com.kivojenko.plugin.manage_scheduled_events.service.QuartzTrigger;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/** {@link QuartzDatabase} on top of the Database plugin's data sources and connections. Only reads. */
public final class QuartzDatabaseImpl implements QuartzDatabase {
    private static final String JOB_DETAILS = "job_details";
    private static final String TRIGGERS = "triggers";
    private static final String CRON_TRIGGERS = "cron_triggers";
    private static final String SIMPLE_TRIGGERS = "simple_triggers";

    private final Project project;

    public QuartzDatabaseImpl(Project project) {
        this.project = project;
    }

    @Override
    public List<Source> findSources() {
        List<Source> sources = new ArrayList<>();
        for (var dataSource : DbPsiFacade.getInstance(project).getDataSources()) {
            if (!(dataSource.getDelegate() instanceof LocalDataSource local)) continue;

            // Table names by schema, to find the Quartz tables side by side; lower-cased as databases differ in case
            Map<DasObject, Set<String>> tablesBySchema = new HashMap<>();
            Map<String, String> originalNames = new HashMap<>();
            for (var table : DasUtil.getTables(dataSource)) {
                var name = table.getName().toLowerCase(Locale.ROOT);
                tablesBySchema.computeIfAbsent(table.getDasParent(), k -> new HashSet<>()).add(name);
                originalNames.put(name, table.getName());
            }

            for (var entry : tablesBySchema.entrySet()) {
                var names = entry.getValue();
                for (var name : names) {
                    if (!name.endsWith(CRON_TRIGGERS)) continue;
                    var prefix = name.substring(0, name.length() - CRON_TRIGGERS.length());
                    if (!names.containsAll(List.of(prefix + JOB_DETAILS, prefix + TRIGGERS, prefix + SIMPLE_TRIGGERS))) {
                        continue;
                    }
                    var schema = entry.getKey() == null ? null : entry.getKey().getName();
                    var originalPrefix = originalNames.get(name).substring(0, prefix.length());
                    sources.add(new Source(local.getUniqueId(), dataSource.getName(), schema, originalPrefix));
                }
            }
        }
        return sources;
    }

    @Override
    public List<QuartzTrigger> read(Source source) throws Exception {
        var dataSource = findDataSource(source.id());
        if (dataSource == null) throw new IllegalStateException("Data source " + source.name() + " no longer exists");

        var ref = DatabaseConnectionManager.getInstance()
                .build(project, dataSource)
                .setRequestor(new ConnectionRequestor.Anonymous())
                .setAskPassword(true)
                .createBlocking();
        try {
            var statement = ref.get().getRemoteConnection().createStatement();
            try {
                var resultSet = statement.executeQuery(query(source));
                try {
                    List<QuartzTrigger> triggers = new ArrayList<>();
                    while (resultSet.next()) triggers.add(toTrigger(resultSet, source.name()));
                    return triggers;
                } finally {
                    resultSet.close();
                }
            } finally {
                statement.close();
            }
        } finally {
            ref.close();
        }
    }

    private @Nullable LocalDataSource findDataSource(String id) {
        for (var dataSource : DbPsiFacade.getInstance(project).getDataSources()) {
            if (dataSource.getDelegate() instanceof LocalDataSource local && id.equals(local.getUniqueId())) {
                return local;
            }
        }
        return null;
    }

    /** Every job, once per trigger; jobs without a trigger once with no trigger. */
    static String query(Source source) {
        return """
                SELECT j.JOB_NAME, j.JOB_GROUP, j.JOB_CLASS_NAME,
                       t.TRIGGER_NAME, t.TRIGGER_TYPE, t.TRIGGER_STATE, t.NEXT_FIRE_TIME,
                       c.CRON_EXPRESSION, s.REPEAT_INTERVAL
                FROM %s j
                LEFT JOIN %s t
                       ON t.SCHED_NAME = j.SCHED_NAME AND t.JOB_NAME = j.JOB_NAME AND t.JOB_GROUP = j.JOB_GROUP
                LEFT JOIN %s c
                       ON c.SCHED_NAME = t.SCHED_NAME AND c.TRIGGER_NAME = t.TRIGGER_NAME
                      AND c.TRIGGER_GROUP = t.TRIGGER_GROUP
                LEFT JOIN %s s
                       ON s.SCHED_NAME = t.SCHED_NAME AND s.TRIGGER_NAME = t.TRIGGER_NAME
                      AND s.TRIGGER_GROUP = t.TRIGGER_GROUP
                ORDER BY j.JOB_NAME, t.TRIGGER_NAME""".formatted(
                table(source, JOB_DETAILS), table(source, TRIGGERS),
                table(source, CRON_TRIGGERS), table(source, SIMPLE_TRIGGERS));
    }

    /** E.g. {@code public.qrtz_triggers}, or {@code QRTZ_TRIGGERS} where the tables are upper case. */
    private static String table(Source source, String suffix) {
        var prefix = source.prefix();
        var upperCase = !prefix.isEmpty() && prefix.equals(prefix.toUpperCase(Locale.ROOT));
        var schema = source.schema() == null || source.schema().isBlank() ? "" : source.schema() + ".";
        return schema + prefix + (upperCase ? suffix.toUpperCase(Locale.ROOT) : suffix);
    }

    private static QuartzTrigger toTrigger(RemoteResultSet row, String dataSource) throws Exception {
        return new QuartzTrigger(
                text(row.getObject(1)),
                text(row.getObject(2)),
                text(row.getObject(3)),
                nullableText(row.getObject(4)),
                nullableText(row.getObject(5)),
                nullableText(row.getObject(6)),
                positiveNumber(row.getObject(7)),
                nullableText(row.getObject(8)),
                positiveNumber(row.getObject(9)),
                dataSource);
    }

    private static String text(@Nullable Object value) {
        return value == null ? "" : value.toString().strip();
    }

    private static @Nullable String nullableText(@Nullable Object value) {
        return value == null ? null : value.toString().strip();
    }

    /** Quartz stores "never" as -1. */
    private static @Nullable Long positiveNumber(@Nullable Object value) {
        return value instanceof Number number && number.longValue() > 0 ? number.longValue() : null;
    }
}
