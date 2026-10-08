package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

public class MicrobiologyV2CaseStructureLiquibaseRollbackTest {
    @Test
    public void realApplicationChangelogPreservesHistoryAndEnforcesActiveOwnership() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.4")) {
            postgres.withCopyFileToContainer(MountableFile.forClasspathResource("postgre-db-init"),
                    "/docker-entrypoint-initdb.d");
            postgres.withEnv("POSTGRES_INITDB_ARGS", "--auth-host=md5");
            postgres.withDatabaseName("clinlims").withUsername("clinlims").withPassword(UUID.randomUUID().toString());
            postgres.start();
            try (Connection connection = postgres.createConnection("")) {
                Database database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                database.setDefaultSchemaName("clinlims");
                ClassLoaderResourceAccessor resources = new ClassLoaderResourceAccessor();
                Liquibase app = new Liquibase("liquibase/base-changelog.xml", resources, database);
                Contexts contexts = new Contexts("test");
                List<ChangeSet> pending = app.listUnrunChangeSets(contexts, new LabelExpression());
                int preceding = 0;
                while (preceding < pending.size()
                        && !pending.get(preceding).getId().equals("012-ogc-1383-case-structure"))
                    preceding++;
                assertTrue("V2 case changes must be registered in the application changelog",
                        preceding < pending.size());
                app.update(preceding, contexts, new LabelExpression());
                assertFalse(columnExists(connection, "micro_case", "lab_unit_id"));
                seedStoredHistory(connection);
                String originalCase = scalar(connection,
                        "SELECT to_jsonb(c)::text FROM clinlims.micro_case c WHERE id='stored-case'");
                String originalResult = scalar(connection,
                        "SELECT row_to_json(r)::text FROM clinlims.result r WHERE id=888803");
                String originalEvent = scalar(connection,
                        "SELECT row_to_json(e)::text FROM clinlims.micro_case_activity e WHERE id='stored-event'");

                execute(connection,
                        "INSERT INTO clinlims.micro_case (id,sample_item_id,workflow_type) VALUES ('ambiguous-case',888802,'MYCOLOGY')");
                execute(connection,
                        "INSERT INTO clinlims.micro_case_analysis (id,case_id,analysis_id) VALUES ('ambiguous-link','ambiguous-case',888802)");
                if (!connection.getAutoCommit())
                    connection.commit();
                assertEquals("2", scalar(connection,
                        "SELECT COUNT(*) FROM clinlims.micro_case_analysis WHERE analysis_id=888802"));
                try {
                    app.update(contexts);
                    fail("Historical ownership must be reviewed, never chosen by the migration");
                } catch (LiquibaseException expected) {
                    assertFalse(columnExists(connection, "micro_case", "lab_unit_id"));
                    assertEquals("2", scalar(connection,
                            "SELECT COUNT(*) FROM clinlims.micro_case_analysis WHERE analysis_id=888802"));
                }
                // Remove only the deliberately conflicting test fixture; production has no
                // automatic resolution.
                execute(connection, "DELETE FROM clinlims.micro_case_analysis WHERE id='ambiguous-link'");
                execute(connection, "DELETE FROM clinlims.micro_case WHERE id='ambiguous-case'");
                if (!connection.getAutoCommit())
                    connection.commit();
                app.update(contexts);
                assertTrue(columnExists(connection, "micro_case_request", "sample_type_request_id"));
                assertTrue(columnExists(connection, "micro_case_split", "source_case_id"));
                assertEquals(originalCase, scalar(connection,
                        "SELECT (to_jsonb(c) - ARRAY['sample_id','sample_type_id','lab_unit_id','program_id','status'])::text FROM clinlims.micro_case c WHERE id='stored-case'"));
                assertEquals("BACTERIOLOGY",
                        scalar(connection, "SELECT workflow_type FROM clinlims.micro_case WHERE id='stored-case'"));
                assertEquals("888802", scalar(connection,
                        "SELECT sample_item_id::text FROM clinlims.micro_case WHERE id='stored-case'"));
                assertEquals("0", scalar(connection,
                        "SELECT COUNT(*) FROM clinlims.micro_case WHERE id='stored-case' AND (lab_unit_id IS NOT NULL OR program_id IS NOT NULL OR sample_id IS NOT NULL OR status IS NOT NULL)"));
                assertPreservedFields(originalResult,
                        scalar(connection, "SELECT row_to_json(r)::text FROM clinlims.result r WHERE id=888803"));
                assertEquals(originalEvent, scalar(connection,
                        "SELECT row_to_json(e)::text FROM clinlims.micro_case_activity e WHERE id='stored-event'"));
                assertNull(scalar(connection,
                        "SELECT case_role FROM clinlims.micro_case_analysis WHERE id='stored-link'"));
                verifyConstraints(connection);

                assertTrue(columnExists(connection, "sample_type_request", "culture_set_number"));
                assertTrue(columnExists(connection, "sample_item", "body_site"));
                assertTrue(columnExists(connection, "dictionary", "container_population"));
                assertTrue(columnExists(connection, "test", "opens_microbiology_case"));
                assertNull(scalar(connection, "SELECT culture_set_number FROM clinlims.sample_item WHERE id=888802"));
                assertNull(scalar(connection, "SELECT body_site FROM clinlims.sample_item WHERE id=888802"));
                // Shared details and catalog flags are registered, reversible application
                // changes.
                new Liquibase("liquibase/3.6.x.x/014-sample-collection-set-details.xml", resources, database)
                        .rollback(2, "test");
                assertFalse(columnExists(connection, "sample_item", "culture_set_number"));
                assertFalse(columnExists(connection, "dictionary", "container_population"));
                new Liquibase("liquibase/3.6.x.x/013-microbiology-v2-routing.xml", resources, database).rollback(2,
                        "test");
                assertFalse(columnExists(connection, "test", "opens_microbiology_case"));
                assertPreservedFields(originalResult,
                        scalar(connection, "SELECT row_to_json(r)::text FROM clinlims.result r WHERE id=888803"));
                app.update(contexts);
                assertTrue(columnExists(connection, "sample_item", "culture_set_number"));

                Liquibase feature = new Liquibase("liquibase/3.6.x.x/012-microbiology-v2-case-structure.xml", resources,
                        database);
                feature.rollback(3, "test");
                assertFalse(columnExists(connection, "micro_case", "lab_unit_id"));
                assertEquals(originalCase, scalar(connection,
                        "SELECT to_jsonb(c)::text FROM clinlims.micro_case c WHERE id='stored-case'"));
                app.update(contexts);
                assertTrue(columnExists(connection, "micro_case", "lab_unit_id"));
            }
        }
    }

    private void assertPreservedFields(String before, String after) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode oldRow = mapper.readTree(before);
        JsonNode upgradedRow = mapper.readTree(after);
        oldRow.fields().forEachRemaining(field -> assertEquals("Stored field " + field.getKey(), field.getValue(),
                upgradedRow.get(field.getKey())));
    }

    private void seedStoredHistory(Connection c) throws Exception {
        execute(c,
                "INSERT INTO clinlims.sample (id,accession_number,entered_date,received_date) VALUES (888801,'V2-UPGRADE',DATE '2026-01-01',TIMESTAMP '2026-01-02 09:00:00')");
        execute(c,
                "INSERT INTO clinlims.sample_item (id,sort_order,samp_id,status_id) SELECT 888802,1,888801,MIN(id) FROM clinlims.status_of_sample");
        execute(c, "INSERT INTO clinlims.analysis (id,sampitem_id,analysis_type) VALUES (888802,888802,'MANUAL')");
        execute(c,
                "INSERT INTO clinlims.result (id,analysis_id,result_type,value) VALUES (888803,888802,'A','Stored positive result')");
        execute(c,
                "INSERT INTO clinlims.micro_case (id,sample_item_id,workflow_type,created_by) VALUES ('stored-case',888802,'BACTERIOLOGY',(SELECT MIN(id) FROM clinlims.system_user))");
        execute(c,
                "INSERT INTO clinlims.micro_case_analysis (id,case_id,analysis_id,projected_result_id) VALUES ('stored-link','stored-case',888802,888803)");
        execute(c,
                "INSERT INTO clinlims.micro_case_activity (id,case_id,activity_type,performed_by,note) VALUES ('stored-event','stored-case','MANUAL_NOTE',(SELECT MIN(id) FROM clinlims.system_user),'Unchanged history')");
    }

    private void verifyConstraints(Connection c) throws Exception {
        c.setAutoCommit(false);
        try {
            execute(c, "INSERT INTO clinlims.micro_case (id,sample_id) VALUES ('case-a',888801),('case-b',888801)");
            execute(c,
                    "INSERT INTO clinlims.micro_case_sample (id,case_id,sample_item_id,joined_at,joined_by) VALUES ('member-a','case-a',888802,now(),(SELECT MIN(id) FROM clinlims.system_user)::text),('member-b','case-b',888802,now(),(SELECT MIN(id) FROM clinlims.system_user)::text)");
            rejectUnique(c,
                    "INSERT INTO clinlims.micro_case_sample (id,case_id,sample_item_id,joined_at,joined_by) VALUES ('duplicate-member','case-a',888802,now(),(SELECT MIN(id) FROM clinlims.system_user)::text)");
            rejectUnique(c,
                    "INSERT INTO clinlims.micro_case_analysis (id,case_id,analysis_id) VALUES ('duplicate-link','case-b',888802)");
            execute(c,
                    "UPDATE clinlims.micro_case_analysis SET cancelled_at=now(),cancelled_by=(SELECT MIN(id) FROM clinlims.system_user)::text,cancellation_reason='Retained history' WHERE id='stored-link'");
            execute(c,
                    "INSERT INTO clinlims.micro_case_analysis (id,case_id,analysis_id) VALUES ('new-link','case-b',888802)");
            assertEquals("2", scalar(c, "SELECT COUNT(*) FROM clinlims.micro_case_analysis WHERE analysis_id=888802"));
        } finally {
            c.rollback();
            c.setAutoCommit(true);
        }
    }

    private void rejectUnique(Connection c, String sql) throws Exception {
        Savepoint point = c.setSavepoint();
        try {
            execute(c, sql);
            fail("Duplicate active ownership must fail");
        } catch (SQLException expected) {
            assertEquals("23505", expected.getSQLState());
        } finally {
            c.rollback(point);
        }
    }

    private boolean columnExists(Connection c, String table, String column) throws Exception {
        try (ResultSet columns = c.getMetaData().getColumns(null, "clinlims", table, column)) {
            return columns.next();
        }
    }

    private void execute(Connection c, String sql) throws Exception {
        try (Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private String scalar(Connection c, String sql) throws Exception {
        try (Statement s = c.createStatement(); ResultSet rows = s.executeQuery(sql)) {
            assertTrue(rows.next());
            return rows.getString(1);
        }
    }
}
