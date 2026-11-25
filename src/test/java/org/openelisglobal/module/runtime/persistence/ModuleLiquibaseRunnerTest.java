package org.openelisglobal.module.runtime.persistence;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.nio.file.Paths;
import java.util.List;
import javax.sql.DataSource;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import org.junit.Test;
import org.openelisglobal.module.api.ModuleState;
import org.openelisglobal.module.api.manifest.ModuleManifest;
import org.openelisglobal.module.manifest.ManifestReader;
import org.openelisglobal.module.runtime.ContractVersions;
import org.openelisglobal.module.runtime.ModuleRecord;
import org.openelisglobal.module.runtime.ModuleRegistry;
import org.openelisglobal.module.runtime.ModuleStartupException;
import org.openelisglobal.module.runtime.ModuleSystemProperties;
import org.openelisglobal.module.testkit.ModuleRecords;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;

public class ModuleLiquibaseRunnerTest {

    private static ModuleRecord module(String id) {
        ModuleManifest manifest = new ModuleManifest();
        manifest.setId(id);
        manifest.setKind("module");
        manifest.setName(id);
        manifest.setVersion("1.0.0");
        return ModuleRecords.record(Paths.get(id + ".jar"), manifest);
    }

    private static DatabaseChangeLog changelog(String... idsAndAuthors) {
        DatabaseChangeLog log = new DatabaseChangeLog("liquibase/x/changelog.xml");
        for (int i = 0; i < idsAndAuthors.length; i += 2) {
            log.addChangeSet(new ChangeSet(idsAndAuthors[i], idsAndAuthors[i + 1], false, false,
                    "liquibase/x/changelog.xml", null, null, log));
        }
        return log;
    }

    @Test
    public void prefixedIdsWithTheModuleAsAuthorPass() {
        ModuleLiquibaseRunner.checkChangeSets(module("follow-up"),
                changelog("follow-up-001-table", "follow-up", "follow-up-002-index", "follow-up"));
    }

    @Test
    public void unprefixedIdIsRefusedBeforeAnythingRuns() {
        try {
            ModuleLiquibaseRunner.checkChangeSets(module("follow-up"), changelog("001-table", "follow-up"));
            fail("expected refusal");
        } catch (ModuleStartupException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("follow-up-"));
        }
    }

    @Test
    public void wrongAuthorIsRefused() {
        try {
            ModuleLiquibaseRunner.checkChangeSets(module("follow-up"),
                    changelog("follow-up-001-table", "openelisglobal"));
            fail("expected refusal");
        } catch (ModuleStartupException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("author"));
        }
    }

    @Test
    public void aModuleDiscoverySetAsideGetsItsStateRecordedWithoutMigrating() {
        ModuleRecord sleepy = ModuleRecords.record(Paths.get("/modules/sleepy-1.0.0.jar"), new ManifestReader().read(
                "{\"schemaVersion\":1,\"id\":\"sleepy\",\"kind\":\"module\",\"name\":\"Sleepy\",\"version\":\"1.0.0\","
                        + "\"requires\":{\"openelis\":\">=3.4.0\"}}"));
        sleepy.transition(ModuleState.DISABLED, "switched off by modules.sleepy.enabled=false");
        ModuleRegistry registry = new ModuleRegistry(new ModuleSystemProperties(new MockEnvironment()),
                new ContractVersions("3.4.0", "1.0.0", "1.0.0"), List.of(sleepy));
        JdbcTemplate jdbc = mock(JdbcTemplate.class);

        new ModuleLiquibaseRunner(registry, mock(DataSource.class), "test")
                .run(new ModuleLiquibaseRunner.ModuleStateRepository(jdbc));

        verify(jdbc).update("update clinlims.module_state set status = ?, last_error = ? where id = ?", "DISABLED",
                "switched off by modules.sleepy.enabled=false", "sleepy");
        verifyNoMoreInteractions(jdbc);
    }
}
