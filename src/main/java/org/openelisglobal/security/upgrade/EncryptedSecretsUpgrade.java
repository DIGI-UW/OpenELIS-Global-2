package org.openelisglobal.security.upgrade;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.core.RawSqlStatement;
import liquibase.statement.core.UpdateStatement;
import org.jasypt.util.text.AES256TextEncryptor;
import org.openelisglobal.security.GeneralTextEncryptor;

/**
 * Explicit, resumable upgrade; never registered in Spring or the normal
 * changelog.
 */
public class EncryptedSecretsUpgrade implements CustomTaskChange {
    private static final int BATCH_SIZE = 250;
    private static final List<Column> COLUMNS = List.of(new Column("delivery_channel", "secret", ""),
            new Column("eqa_panel_sample", "target_value", ""), new Column("basic_authentication_data", "password", ""),
            new Column("client_results_view", "password", ""),
            new Column("site_information", "value", " AND encrypted"),
            new Column("analyzer_bridge_pairing", "client_private_key", ""));
    private String previousKey;
    private String installKey;
    private int rewritten;

    private record Column(String table, String name, String condition) {
    }

    public void setPreviousKey(String previousKey) {
        this.previousKey = previousKey;
    }

    public void setInstallKey(String installKey) {
        this.installKey = installKey;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            throw new IllegalArgumentException(
                    "Supply upgrade settings through environment and key files, not arguments");
        }
        run(System.getenv());
    }

    static void run(Map<String, String> environment) throws Exception {
        ResourceAccessor resources = new ClassLoaderResourceAccessor();
        Database database = DatabaseFactory.getInstance().openDatabase(required(environment, "OE_UPGRADE_DB_URL"),
                required(environment, "OE_UPGRADE_DB_USER"), readSecret(environment, "OE_UPGRADE_DB_PASSWORD_FILE"),
                null, resources);
        try (Liquibase upgrade = new Liquibase("liquibase/3.3.x.x/upgrade-encryption.xml", resources, database)) {
            Scope.child(
                    Map.of("oeUpgradePreviousKey", readSecret(environment, "OE_UPGRADE_PREVIOUS_KEY_FILE"),
                            "oeUpgradeInstallKey", readSecret(environment, "OE_UPGRADE_INSTALL_KEY_FILE")),
                    () -> upgrade.update(new Contexts()));
        }
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Set " + name);
        }
        return value;
    }

    private static String readSecret(Map<String, String> environment, String name) throws Exception {
        String value = Files.readString(Path.of(required(environment, name)));
        // Match entrypoint command substitution: trailing LF is a file terminator;
        // spaces, tabs, CR and other key bytes remain significant.
        while (value.endsWith("\n")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Empty secret file: " + name);
        }
        return value;
    }

    @Override
    public void execute(Database database) throws CustomChangeException {
        if (previousKey == null) {
            previousKey = Scope.getCurrentScope().get("oeUpgradePreviousKey", String.class);
        }
        if (installKey == null) {
            installKey = Scope.getCurrentScope().get("oeUpgradeInstallKey", String.class);
        }
        if (previousKey == null || previousKey.isBlank() || installKey == null || installKey.isBlank()) {
            throw new CustomChangeException("Both previous and installation keys are required");
        }
        AES256TextEncryptor previous = new AES256TextEncryptor();
        previous.setPassword(previousKey);
        GeneralTextEncryptor current = GeneralTextEncryptor.of(installKey, null);
        Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor("jdbc", database);
        try {
            database.setAutoCommit(false);
            for (Column column : COLUMNS) {
                // Older schemas may not yet contain optional features. Only migrate columns
                // that exist.
                if (executor.queryForInt(new RawSqlStatement("SELECT count(*) FROM information_schema.columns"
                        + " WHERE table_schema = 'clinlims' AND table_name = '" + column.table()
                        + "' AND column_name = '" + column.name() + "'")) == 0) {
                    continue;
                }
                BigDecimal after = BigDecimal.valueOf(-1);
                while (true) {
                    List<Map<String, ?>> rows = executor.queryForList(
                            new RawSqlStatement("SELECT id, " + column.name() + " AS ciphertext FROM clinlims."
                                    + column.table() + " WHERE id > " + after.toPlainString() + " AND " + column.name()
                                    + " IS NOT NULL" + column.condition() + " ORDER BY id LIMIT " + BATCH_SIZE));
                    if (rows.isEmpty()) {
                        break;
                    }
                    for (Map<String, ?> row : rows) {
                        Map<String, Object> fields = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                        fields.putAll(row);
                        after = new BigDecimal(fields.get("id").toString());
                        String ciphertext = (String) fields.get("ciphertext");
                        if (ciphertext.isBlank()) {
                            continue;
                        }
                        if (ciphertext.startsWith(GeneralTextEncryptor.INSTALL_KEY_PREFIX)) {
                            current.decrypt(ciphertext);
                            continue;
                        }
                        String replacement = current.encrypt(previous.decrypt(ciphertext));
                        rewritten += executor.update(new UpdateStatement(null, "clinlims", column.table())
                                .addNewColumnValue(column.name(), replacement).setWhereClause("id = ?")
                                .addWhereParameter(after));
                    }
                    database.commit();
                }
            }
            database.commit();
        } catch (Exception failure) {
            try {
                database.rollback();
            } catch (Exception rollback) {
                failure.addSuppressed(rollback);
            }
            throw new CustomChangeException("Encryption upgrade failed; restore the correct keys and rerun. "
                    + "Completed batches are retained; the current batch was rolled back. Failure type: "
                    + failure.getClass().getSimpleName());
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "Upgraded " + rewritten + " encrypted values";
    }

    @Override
    public void setUp() {
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }
}
