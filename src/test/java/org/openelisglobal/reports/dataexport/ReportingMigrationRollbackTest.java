package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Executes the shipped migrations on dedicated databases, never the shared test
 * application.
 */
public class ReportingMigrationRollbackTest {
    private static final String ALL = "liquibase/reporting-mvp-rollback.xml";
    private static final String M1 = "liquibase/3.6.x.x/003-configurable-reporting.xml";
    private static final String M2 = "liquibase/3.6.x.x/004-reporting-recovery.xml";
    private static final String MENU = "liquibase/3.6.x.x/005-menu-presentation.xml";

    @Test
    public void freshDatabaseRegistersReportingAndCanUninstallAndReapplyIt() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.4")) {
            initialize(postgres);
            try (Connection connection = postgres.createConnection("")) {
                assertEquals(0, count(connection, "SELECT count(*) FROM clinlims.reporting_export_job"));
                assertEquals(4, appliedChanges(connection));
                assertEquals(Set.of("reporting_export_job_pkey", "reporting_job_submission_unique",
                        "reporting_job_owner_submitted", "reporting_job_state_submitted", "reporting_job_expiry",
                        "reporting_job_cleanup"), jobIndexes(connection));
                String reports = definitionsFingerprint(connection);
                String otherMenus = otherMenusFingerprint(connection);

                migrate(postgres, ALL, 4);
                assertEquals(0, tableCount(connection, "reporting_export_job"));
                assertEquals(0, columnCount(connection, "report_definition", "updated_by"));
                assertEquals(0, exportMenuCount(connection));
                assertEquals(0, appliedChanges(connection));
                assertEquals(reports, definitionsFingerprint(connection));
                assertEquals(otherMenus, otherMenusFingerprint(connection));

                migrate(postgres, ALL, 0);
                migrate(postgres, ALL, 0);
                assertEquals(4, appliedChanges(connection));
                assertEquals(1, exportMenuCount(connection));
                assertEquals(1, columnCount(connection, "report_definition", "updated_by"));
                assertEquals(1, columnCount(connection, "reporting_export_job", "output_cleaned_at"));
                assertEquals(0, count(connection, "SELECT count(*) FROM clinlims.reporting_export_job"));
                assertEquals(reports, definitionsFingerprint(connection));
                assertEquals(otherMenus, otherMenusFingerprint(connection));
            }
        }
    }

    @Test
    public void populatedRecoveryUpgradeAndRollbackRetainEveryJobAndSharedDefinition() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.4")) {
            initialize(postgres);
            migrate(postgres, ALL, 4);
            try (Connection connection = postgres.createConnection("")) {
                // Pre-existing report payloads must survive adding updated_by to the shared
                // table.
                execute(connection, """
                        INSERT INTO clinlims.report_definition
                          (id, name, definition_json, created_by, last_updated, report_type, is_public)
                        SELECT 'MIG-LEGACY-' || i, 'Legacy ' || i,
                          json_build_object('title', 'Résumé, "quoted"', 'ordinal', i)::text,
                          'legacy-owner', timestamp '2026-05-01 08:00:00', 'PATIENT', true
                        FROM generate_series(1,1000) i
                        """);
                String beforeM1 = definitionsFingerprint(connection);
                String otherMenus = otherMenusFingerprint(connection);
                migrate(postgres, M1, 0);
                assertEquals(beforeM1, definitionsFingerprint(connection));
                seedReportingHistory(connection);
                assertEquals(50000, count(connection, "SELECT count(*) FROM clinlims.reporting_export_job"));
                assertEquals(100, count(connection,
                        "SELECT count(*) FROM clinlims.report_definition WHERE report_type='CSV_SAVED'"));
                assertSubmissionUniqueness(connection);
                String jobs = jobsFingerprint(connection);
                String reports = completeDefinitionsFingerprint(connection);

                long began = System.nanoTime();
                migrate(postgres, M2, 0);
                System.out.println("Reporting recovery upgrade over 50,000 jobs: "
                        + (System.nanoTime() - began) / 1_000_000 + " ms");
                assertEquals(jobs, jobsFingerprint(connection));
                assertEquals(reports, completeDefinitionsFingerprint(connection));
                assertEquals(1, count(connection, """
                        SELECT count(*) FROM pg_index i JOIN pg_class c ON c.oid=i.indexrelid
                        JOIN pg_namespace n ON n.oid=c.relnamespace
                        WHERE n.nspname='clinlims' AND c.relname='reporting_job_cleanup' AND i.indisvalid
                        """));
                assertEquals("timestamp with time zone", scalar(connection, """
                        SELECT data_type FROM information_schema.columns WHERE table_schema='clinlims'
                        AND table_name='reporting_export_job' AND column_name='output_cleaned_at'
                        """));
                execute(connection, """
                        UPDATE clinlims.reporting_export_job SET output_cleaned_at=timestamptz '2026-05-09 10:00:00+00'
                        WHERE state='EXPIRED'
                        """);
                assertEquals(8333, count(connection,
                        "SELECT count(*) FROM clinlims.reporting_export_job WHERE output_cleaned_at IS NOT NULL"));

                migrate(postgres, M2, 1);
                assertEquals(0, columnCount(connection, "reporting_export_job", "output_cleaned_at"));
                assertEquals(5, jobIndexes(connection).size());
                assertEquals(jobs, jobsFingerprint(connection));
                assertEquals(reports, completeDefinitionsFingerprint(connection));
                assertEquals(50000, count(connection, "SELECT count(*) FROM clinlims.reporting_export_job"));
                assertSubmissionUniqueness(connection);

                migrate(postgres, M2, 0);
                assertEquals(jobs, jobsFingerprint(connection));
                assertEquals(reports, completeDefinitionsFingerprint(connection));
                assertEquals(0, count(connection,
                        "SELECT count(*) FROM clinlims.reporting_export_job WHERE output_cleaned_at IS NOT NULL"));
                assertEquals(otherMenus, otherMenusFingerprint(connection));
                assertEquals(4, appliedChanges(connection));
            }
        }
    }

    private void initialize(PostgreSQLContainer<?> postgres) throws Exception {
        initialize(postgres, false);
    }

    private void initialize(PostgreSQLContainer<?> postgres, boolean retainMenuPresentation) throws Exception {
        postgres.withCopyFileToContainer(MountableFile.forClasspathResource("postgre-db-init"),
                "/docker-entrypoint-initdb.d");
        postgres.withEnv("POSTGRES_INITDB_ARGS", "--auth-host=md5");
        postgres.withDatabaseName("clinlims").withUsername("clinlims").withPassword("clinlims");
        postgres.start();
        // Full application changelog proves that the real versioned includes are
        // registered.
        migrate(postgres, "liquibase/base-changelog.xml", 0);
        // Reporting rollback remains scoped to its four changesets. The menu
        // migration is qualified independently below, including full registration.
        if (!retainMenuPresentation) {
            migrate(postgres, MENU, 1);
        }
    }

    @Test
    public void menuPresentationUpgradeAndRollbackPreserveAllExistingMenuFields() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.4")) {
            initialize(postgres, true);
            try (Connection connection = postgres.createConnection("")) {
                assertEquals(1, columnCount(connection, "menu", "presentation_style"));
                assertEquals(1, columnCount(connection, "menu", "icon"));
                assertEquals(1, columnCount(connection, "menu", "last_updated"));
                migrate(postgres, MENU, 1);
                execute(connection, """
                        INSERT INTO clinlims.menu
                          (id,element_id,presentation_order,display_key,action_url,is_active)
                        SELECT 900000+i,'migration-menu-'||i,i,'instance.menu',
                          '/Report?instance='||i, i%2=0 FROM generate_series(1,1000) i
                        """);
                String before = otherMenusFingerprint(connection);
                migrate(postgres, MENU, 0);
                assertEquals(before, legacyMenuFieldsFingerprint(connection));
                assertEquals(1000, count(connection, """
                        SELECT count(*) FROM clinlims.menu WHERE element_id LIKE 'migration-menu-%'
                        AND presentation_style IS NULL AND icon IS NULL AND last_updated IS NOT NULL
                        """));
                execute(connection, """
                        UPDATE clinlims.menu SET presentation_style='section',icon='reports'
                        WHERE element_id='migration-menu-1'
                        """);
                assertEquals(before, legacyMenuFieldsFingerprint(connection));
                migrate(postgres, MENU, 1);
                assertEquals(0, columnCount(connection, "menu", "presentation_style"));
                assertEquals(0, columnCount(connection, "menu", "icon"));
                assertEquals(0, columnCount(connection, "menu", "last_updated"));
                assertEquals(before, otherMenusFingerprint(connection));
                migrate(postgres, MENU, 0);
                migrate(postgres, MENU, 0);
                assertEquals(before, legacyMenuFieldsFingerprint(connection));
                assertEquals(5, appliedChanges(connection));
            }
        }
    }

    private String legacyMenuFieldsFingerprint(Connection connection) throws Exception {
        return scalar(connection, """
                SELECT md5(string_agg((to_jsonb(m)-'presentation_style'-'icon'-'last_updated')::text,'' ORDER BY id))
                FROM clinlims.menu m WHERE element_id <> 'menu_reports_custom_data_export'
                """);
    }

    private void migrate(PostgreSQLContainer<?> postgres, String changelog, int rollbackCount) throws Exception {
        try (Connection connection = postgres.createConnection("")) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName("clinlims");
            try (Liquibase liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(), database)) {
                if (rollbackCount == 0) {
                    liquibase.update(new Contexts("test"));
                } else {
                    liquibase.rollback(rollbackCount, "test");
                }
            }
        }
    }

    private void seedReportingHistory(Connection connection) throws Exception {
        execute(connection, """
                INSERT INTO clinlims.report_definition
                  (id,name,definition_json,created_by,updated_by,last_updated,report_type,is_public)
                SELECT 'MIG-SAVED-' || i, 'Shared ' || i,
                  json_build_object('schemaVersion',1,'reportType','SAMPLE_TESTING','layout','SPREADSHEET',
                    'selectedVariables',json_build_array('accessionNumber','test:174'))::text,
                  'original-author','last-editor',timestamp '2026-05-07 10:00:00','CSV_SAVED',true
                FROM generate_series(1,100) i
                """);
        execute(connection, """
                INSERT INTO clinlims.reporting_export_job
                  (id,owner_id,client_request_id,source_id,layout,request_json,request_hash,submitted_at,
                   parent_id,state,started_at,completed_at,expires_at,worker_id,lease_until,row_count,
                   file_size,failure_code,last_updated)
                SELECT '47960000-0000-4000-8001-' || lpad(i::text,12,'0'), 'report-owner', 'migration-' || i,
                  'SAMPLE_TESTING','SPREADSHEET',
                  json_build_object('schemaVersion',1,'selectedVariables',json_build_array('accessionNumber'),
                    'capturedLabel','Résumé, "quoted"','ordinal',i)::text, md5(i::text),
                  timestamp '2026-05-07 09:00:00',
                  CASE WHEN i > 1 THEN '47960000-0000-4000-8001-' || lpad((i-1)::text,12,'0') END,
                  (ARRAY['QUEUED','GENERATING','READY','FAILED','EXPIRED','CANCELLED'])[1+(i-1)%6],
                  timestamp '2026-05-07 10:00:00',timestamp '2026-05-07 10:05:00',
                  timestamp '2026-05-14 10:05:00','migration-worker',timestamp '2026-05-07 10:01:00',
                  i, i*100, CASE WHEN (i-1)%6=3 THEN 'reporting.job.interrupted' END,
                  timestamp '2026-05-07 10:05:00'
                FROM generate_series(1,50000) i
                """);
    }

    private void assertSubmissionUniqueness(Connection connection) throws Exception {
        SQLException duplicate = assertThrows(SQLException.class, () -> execute(connection,
                """
                        INSERT INTO clinlims.reporting_export_job
                          (id,owner_id,client_request_id,source_id,layout,request_json,request_hash,submitted_at,state)
                        VALUES ('duplicate','report-owner','migration-1','SAMPLE_TESTING','SPREADSHEET','{}','hash',now(),'QUEUED')
                        """));
        assertEquals("23505", duplicate.getSQLState());
    }

    private String definitionsFingerprint(Connection connection) throws Exception {
        return scalar(connection, """
                SELECT md5(string_agg((to_jsonb(d)-'updated_by')::text,'' ORDER BY id))
                FROM clinlims.report_definition d
                """);
    }

    private String completeDefinitionsFingerprint(Connection connection) throws Exception {
        return scalar(connection, """
                SELECT md5(string_agg(to_jsonb(d)::text,'' ORDER BY id)) FROM clinlims.report_definition d
                """);
    }

    private String jobsFingerprint(Connection connection) throws Exception {
        // Recovery owns only the cleanup marker; every M1 field must remain
        // byte-for-byte unchanged.
        return scalar(connection, """
                SELECT md5(string_agg(md5((to_jsonb(j)-'output_cleaned_at')::text),'' ORDER BY id))
                FROM clinlims.reporting_export_job j
                """);
    }

    private String otherMenusFingerprint(Connection connection) throws Exception {
        return scalar(connection, """
                SELECT md5(string_agg(to_jsonb(m)::text,'' ORDER BY id)) FROM clinlims.menu m
                WHERE element_id <> 'menu_reports_custom_data_export'
                """);
    }

    private int appliedChanges(Connection connection) throws Exception {
        return count(connection, "SELECT count(*) FROM clinlims.databasechangelog WHERE id LIKE '479-%'");
    }

    private int exportMenuCount(Connection connection) throws Exception {
        return count(connection,
                "SELECT count(*) FROM clinlims.menu WHERE element_id='menu_reports_custom_data_export'");
    }

    private int tableCount(Connection connection, String table) throws Exception {
        return count(connection, "SELECT count(*) FROM information_schema.tables WHERE table_schema='clinlims'"
                + " AND table_name='" + table + "'");
    }

    private int columnCount(Connection connection, String table, String column) throws Exception {
        return count(connection, "SELECT count(*) FROM information_schema.columns WHERE table_schema='clinlims'"
                + " AND table_name='" + table + "' AND column_name='" + column + "'");
    }

    private Set<String> jobIndexes(Connection connection) throws Exception {
        Set<String> names = new HashSet<>();
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT indexname FROM pg_indexes WHERE schemaname='clinlims' AND tablename='reporting_export_job'")) {
            while (result.next())
                names.add(result.getString(1));
        }
        return names;
    }

    private int count(Connection connection, String sql) throws Exception {
        return Integer.parseInt(scalar(connection, sql));
    }

    private String scalar(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
