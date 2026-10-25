package org.openelisglobal.security.upgrade;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import org.jasypt.util.text.AES256TextEncryptor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openelisglobal.security.GeneralTextEncryptor;
import org.testcontainers.containers.PostgreSQLContainer;

public class EncryptedSecretsUpgradeTest {
    @Rule
    public TemporaryFolder files = new TemporaryFolder();

    @Test
    public void interruptedUpgradeResumesByPrimaryKeyAndCompletesOnlyOnce() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.4")
                .withPassword(" " + java.util.UUID.randomUUID() + " ")) {
            postgres.start();
            Path previous = secret("previous", " previous-key ");
            Path current = secret("current", " install-key ");
            Path password = secret("password", postgres.getPassword());
            Map<String, String> env = Map.of("OE_UPGRADE_DB_URL", postgres.getJdbcUrl(), "OE_UPGRADE_DB_USER",
                    postgres.getUsername(), "OE_UPGRADE_DB_PASSWORD_FILE", password.toString(),
                    "OE_UPGRADE_PREVIOUS_KEY_FILE", previous.toString(), "OE_UPGRADE_INSTALL_KEY_FILE",
                    current.toString());
            AES256TextEncryptor old = new AES256TextEncryptor();
            old.setPassword(" previous-key ");
            try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
                    postgres.getPassword())) {
                connection.createStatement().execute("CREATE SCHEMA clinlims");
                connection.createStatement().execute(
                        "CREATE TABLE clinlims.site_information (id bigint PRIMARY KEY, value text, encrypted boolean)");
                try (PreparedStatement insert = connection
                        .prepareStatement("INSERT INTO clinlims.site_information VALUES (?, ?, true)")) {
                    for (int id = 1; id <= 501; id++) {
                        insert.setInt(1, id);
                        insert.setString(2, id == 251 ? "unreadable" : old.encrypt("secret-" + id));
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                connection.createStatement().execute(
                        "INSERT INTO clinlims.site_information VALUES (502, 'plain setting', false), (503, '', true)");
                assertThrows(Exception.class, () -> EncryptedSecretsUpgrade.run(env));
                assertEquals(250,
                        count(connection, "SELECT count(*) FROM clinlims.site_information WHERE value LIKE 'OE1:%'"));
                assertEquals(0, count(connection,
                        "SELECT count(*) FROM databasechangelog WHERE id = 'installation-encryption-upgrade-v1'"));
                Files.writeString(current, "incorrect-key");
                assertThrows(Exception.class, () -> EncryptedSecretsUpgrade.run(env));
                assertEquals(250,
                        count(connection, "SELECT count(*) FROM clinlims.site_information WHERE value LIKE 'OE1:%'"));
                Files.writeString(current, " install-key ");
                try (PreparedStatement fix = connection
                        .prepareStatement("UPDATE clinlims.site_information SET value = ? WHERE id = 251")) {
                    fix.setString(1, old.encrypt("secret-251"));
                    fix.executeUpdate();
                }
                EncryptedSecretsUpgrade.run(env);
                assertEquals(501,
                        count(connection, "SELECT count(*) FROM clinlims.site_information WHERE value LIKE 'OE1:%'"));
                assertEquals(1, count(connection,
                        "SELECT count(*) FROM databasechangelog WHERE id = 'installation-encryption-upgrade-v1'"));
                GeneralTextEncryptor normal = GeneralTextEncryptor.of(" install-key ", null);
                try (ResultSet values = connection.createStatement()
                        .executeQuery("SELECT id, value FROM clinlims.site_information WHERE id <= 501 ORDER BY id")) {
                    while (values.next()) {
                        assertEquals("secret-" + values.getInt(1), normal.decrypt(values.getString(2)));
                    }
                }
                String before = value(connection, 1);
                EncryptedSecretsUpgrade.run(env);
                assertEquals(before, value(connection, 1));
                assertEquals("plain setting", value(connection, 502));
                assertEquals("", value(connection, 503));
            }
        }
    }

    @Test
    public void freshDatabaseHasNothingToConvert() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.4")
                .withPassword(" " + java.util.UUID.randomUUID() + " ")) {
            postgres.start();
            Map<String, String> env = Map.of("OE_UPGRADE_DB_URL", postgres.getJdbcUrl(), "OE_UPGRADE_DB_USER",
                    postgres.getUsername(), "OE_UPGRADE_DB_PASSWORD_FILE",
                    secret("password", postgres.getPassword()).toString(), "OE_UPGRADE_PREVIOUS_KEY_FILE",
                    secret("previous", " previous-key ").toString(), "OE_UPGRADE_INSTALL_KEY_FILE",
                    secret("current", " install-key ").toString());
            EncryptedSecretsUpgrade.run(env);
        }
    }

    @Test
    public void ciphertextAtTheOldColumnLimitUpgradesAndRemainsWritable() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.4")
                .withPassword(" " + java.util.UUID.randomUUID() + " ")) {
            postgres.start();
            Map<String, String> env = Map.of("OE_UPGRADE_DB_URL", postgres.getJdbcUrl(), "OE_UPGRADE_DB_USER",
                    postgres.getUsername(), "OE_UPGRADE_DB_PASSWORD_FILE",
                    secret("password", postgres.getPassword()).toString(), "OE_UPGRADE_PREVIOUS_KEY_FILE",
                    secret("previous", " previous-key ").toString(), "OE_UPGRADE_INSTALL_KEY_FILE",
                    secret("current", " install-key ").toString());
            AES256TextEncryptor old = new AES256TextEncryptor();
            old.setPassword(" previous-key ");
            GeneralTextEncryptor normal = GeneralTextEncryptor.of(" install-key ", null);
            try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
                    postgres.getPassword())) {
                connection.createStatement().execute("CREATE SCHEMA clinlims");
                connection.createStatement().execute(
                        "CREATE TABLE clinlims.eqa_panel_sample (id bigint PRIMARY KEY, target_value varchar(512))");
                connection.createStatement().execute(
                        "CREATE TABLE clinlims.delivery_channel (id bigint PRIMARY KEY, secret varchar(2048))");
                String target = "x".repeat(336);
                String credential = "y".repeat(1488);
                for (String[] row : new String[][] { { "eqa_panel_sample", "target_value", target, "512" },
                        { "delivery_channel", "secret", credential, "2048" } }) {
                    String ciphertext = old.encrypt(row[2]);
                    assertEquals(Integer.parseInt(row[3]), ciphertext.length());
                    try (PreparedStatement insert = connection
                            .prepareStatement("INSERT INTO clinlims." + row[0] + " VALUES (1, ?)")) {
                        insert.setString(1, ciphertext);
                        insert.executeUpdate();
                    }
                }
                EncryptedSecretsUpgrade.run(env);
                for (String[] row : new String[][] { { "eqa_panel_sample", "target_value", target },
                        { "delivery_channel", "secret", credential } }) {
                    try (ResultSet saved = connection.createStatement()
                            .executeQuery("SELECT " + row[1] + " FROM clinlims." + row[0])) {
                        saved.next();
                        assertEquals(row[2], normal.decrypt(saved.getString(1)));
                    }
                    try (PreparedStatement insert = connection
                            .prepareStatement("INSERT INTO clinlims." + row[0] + " VALUES (2, ?)")) {
                        insert.setString(1, normal.encrypt(row[2]));
                        assertEquals(1, insert.executeUpdate());
                    }
                }
            }
        }
    }

    private Path secret(String name, String value) throws Exception {
        Path file = files.newFile(name).toPath();
        Files.writeString(file, value + "\n");
        return file;
    }

    private int count(Connection connection, String sql) throws Exception {
        try (ResultSet result = connection.createStatement().executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private String value(Connection connection, int id) throws Exception {
        try (ResultSet result = connection.createStatement()
                .executeQuery("SELECT value FROM clinlims.site_information WHERE id=" + id)) {
            result.next();
            return result.getString(1);
        }
    }
}
