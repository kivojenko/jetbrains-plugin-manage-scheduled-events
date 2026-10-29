package com.kivojenko.plugin.manage_scheduled_events.service.database;

import com.kivojenko.plugin.manage_scheduled_events.service.QuartzDatabase;
import junit.framework.TestCase;

public class QuartzDatabaseImplTest extends TestCase {

    public void testQueryKeepsLowerCaseTablesAndSchema() {
        var query = QuartzDatabaseImpl.query(new QuartzDatabase.Source("id", "app", "public", "qrtz_"));

        assertTrue(query.contains("FROM public.qrtz_job_details j"));
        assertTrue(query.contains("LEFT JOIN public.qrtz_cron_triggers c"));
    }

    public void testQueryKeepsUpperCaseTables() {
        var query = QuartzDatabaseImpl.query(new QuartzDatabase.Source("id", "app", null, "QRTZ_"));

        assertTrue(query.contains("FROM QRTZ_JOB_DETAILS j"));
        assertTrue(query.contains("LEFT JOIN QRTZ_SIMPLE_TRIGGERS s"));
    }
}
