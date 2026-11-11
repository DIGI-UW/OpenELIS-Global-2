package org.openelisglobal.module.runtime.persistence;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.module.api.ModuleState;
import org.openelisglobal.module.runtime.ModuleRecord;
import org.openelisglobal.module.runtime.ModuleRegistry;
import org.openelisglobal.module.runtime.ModuleStartupException;
import org.openelisglobal.module.version.VersionRanges;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Runs every validated module's changelog after the core changelog and before
 * the entity manager factory is built, in dependency order, through the
 * Liquibase API with the module's class loader as the resource accessor (the
 * changelog lives inside the module JAR, which only that loader can see).
 * Changesets share the core's {@code databasechangelog} table, so every
 * changeset id must start with {@code <id>-} and carry the module id as its
 * author; the runner refuses a changelog that breaks the rule before touching
 * the database.
 *
 * <p>
 * The installed version of each module is recorded in
 * {@code clinlims.module_state}; a JAR older than the recorded version is
 * refused unless {@code modules.<id>.allowDowngrade=true}. A failing changelog
 * stops start-up naming the module and the changeset, exactly as a failing core
 * changeset does.
 */
public class ModuleLiquibaseRunner {

    private static final String LOG = "ModuleLiquibaseRunner";

    private final ModuleRegistry registry;
    private final DataSource dataSource;
    private final String contexts;

    public ModuleLiquibaseRunner(ModuleRegistry registry, DataSource dataSource, String contexts) {
        this.registry = registry;
        this.dataSource = dataSource;
        this.contexts = contexts;
    }

    /** Migrates every validated module; called once when the bean is created. */
    public void run() {
        if (registry.all().isEmpty()) {
            return;
        }
        run(new ModuleStateRepository(new JdbcTemplate(dataSource)));
    }

    /**
     * Migrates every validated module; a module discovery set aside (disabled,
     * invalid, incompatible) gets its state written to {@code module_state} too, so
     * the row never claims STARTED for a module that is not.
     */
    void run(ModuleStateRepository states) {
        for (ModuleRecord module : registry.all()) {
            if (module.state() == ModuleState.VALIDATED) {
                migrate(module, states);
            } else if (module.state() != ModuleState.DISCOVERED) {
                states.recordSkipped(module);
            }
        }
    }

    private void migrate(ModuleRecord module, ModuleStateRepository states) {
        String recorded = states.installedVersion(module.id()).orElse(null);
        module.setRecordedVersion(recorded);
        if (recorded != null && VersionRanges.isLowerThan(module.version(), recorded)
                && !registry.properties().allowDowngrade(module.id())) {
            module.transition(ModuleState.FAILED, "version " + module.version() + " is older than the installed "
                    + recorded + "; set modules." + module.id() + ".allowDowngrade=true to accept it");
            states.recordFailure(module, module.reason().orElse(""));
            throw new ModuleStartupException("[module:" + module.id() + "] " + module.reason().orElse(""));
        }
        String changelog = module.manifest().getBackend() == null ? null
                : module.manifest().getBackend().getLiquibase();
        if (changelog != null) {
            try {
                applyChangelog(module, changelog);
            } catch (ModuleStartupException e) {
                module.transition(ModuleState.FAILED, e.getMessage());
                states.recordFailure(module, e.getMessage());
                throw e;
            } catch (LiquibaseException | SQLException | IOException | RuntimeException e) {
                String message = "changelog " + changelog + " failed: " + e.getMessage();
                module.transition(ModuleState.FAILED, message);
                states.recordFailure(module, message);
                throw new ModuleStartupException("[module:" + module.id() + "] " + message, e);
            }
        }
        states.recordMigrated(module, registry.versions().openelis());
        module.transition(ModuleState.MIGRATED);
        LogEvent.logInfo(LOG, "migrate",
                "[module:" + module.id() + "] MIGRATED " + module.version() + (recorded == null ? " (first install)"
                        : recorded.equals(module.version()) ? "" : " (from " + recorded + ")"));
    }

    private void applyChangelog(ModuleRecord module, String changelog)
            throws LiquibaseException, SQLException, IOException {
        Connection connection = dataSource.getConnection();
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        try {
            Liquibase liquibase = new Liquibase(changelog, ModuleChangelogs.accessor(module.jarPath()), database);
            DatabaseChangeLog parsed = liquibase.getDatabaseChangeLog();
            checkChangeSets(module, parsed);
            liquibase.update(new Contexts(contexts), new LabelExpression());
        } finally {
            database.close();
        }
    }

    static void checkChangeSets(ModuleRecord module, DatabaseChangeLog changeLog) {
        String prefix = module.id() + "-";
        List<String> problems = new ArrayList<>();
        for (ChangeSet changeSet : changeLog.getChangeSets()) {
            if (changeSet.getId() == null || !changeSet.getId().startsWith(prefix)) {
                problems.add("changeset '" + changeSet.getId() + "' must have an id starting with '" + prefix + "'");
            }
            if (!module.id().equals(changeSet.getAuthor())) {
                problems.add("changeset '" + changeSet.getId() + "' must have author '" + module.id() + "', not '"
                        + changeSet.getAuthor() + "'");
            }
        }
        if (!problems.isEmpty()) {
            throw new ModuleStartupException("changelog refused: " + String.join("; ", problems));
        }
    }

    /** Reads and writes {@code clinlims.module_state}. */
    static final class ModuleStateRepository {

        private final JdbcTemplate jdbc;

        ModuleStateRepository(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        java.util.Optional<String> installedVersion(String moduleId) {
            List<String> versions = jdbc.queryForList("select version from clinlims.module_state where id = ?",
                    String.class, moduleId);
            return versions.isEmpty() ? java.util.Optional.empty() : java.util.Optional.ofNullable(versions.get(0));
        }

        void recordMigrated(ModuleRecord module, String coreVersion) {
            Timestamp now = Timestamp.from(Instant.now());
            int updated = jdbc.update(
                    "update clinlims.module_state set version = ?, status = ?, core_version = ?,"
                            + " last_error = null where id = ?",
                    module.version(), ModuleState.MIGRATED.name(), coreVersion, module.id());
            if (updated == 0) {
                jdbc.update(
                        "insert into clinlims.module_state (id, version, status, core_version, installed_at)"
                                + " values (?, ?, ?, ?, ?)",
                        module.id(), module.version(), ModuleState.MIGRATED.name(), coreVersion, now);
            }
        }

        void recordSkipped(ModuleRecord module) {
            String reason = module.reason().orElse("");
            jdbc.update("update clinlims.module_state set status = ?, last_error = ? where id = ?",
                    module.state().name(), reason.length() > 1000 ? reason.substring(0, 1000) : reason, module.id());
        }

        void recordFailure(ModuleRecord module, String error) {
            String truncated = error.length() > 1000 ? error.substring(0, 1000) : error;
            int updated = jdbc.update("update clinlims.module_state set status = ?, last_error = ? where id = ?",
                    ModuleState.FAILED.name(), truncated, module.id());
            if (updated == 0) {
                jdbc.update(
                        "insert into clinlims.module_state (id, version, status, installed_at, last_error)"
                                + " values (?, ?, ?, ?, ?)",
                        module.id(), module.version(), ModuleState.FAILED.name(), Timestamp.from(Instant.now()),
                        truncated);
            }
        }
    }
}
