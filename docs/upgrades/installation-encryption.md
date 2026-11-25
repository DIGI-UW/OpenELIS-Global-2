# Upgrade an existing database to its installation encryption key

Fresh installations do not run this upgrade. Start `certs` before the webapp;
all new secrets use the generated `install/encryption.key`. Back up that key
with the database. Normal startup neither reads a previous key nor scans secret
columns.

The Linux installer supplies its existing site key through `common.properties`.
Keep that configuration and its Java properties escaping unchanged; the raw
`config/ENCRYPTION_KEY` file can mean a different key if it contains
backslashes. Installer deployments retaining their effective key do not need
data conversion. The application rejects a missing key; no shared default is
supplied.

This explicit upgrade is only for a database containing secrets encrypted with
its previous `encryption.general.password` (historically the keystore password).
It rewrites selected encrypted fields, not the entire database. The application
must remain stopped until it succeeds. Do not change TLS passwords as part of
this step.

1. Stop OpenELIS and every other writer to the database. Take a database backup
   and a matching backup of the existing configuration and key volume.
2. Retain the destination installation key, or deliberately generate a new key
   file for this conversion. Ordinary `certs` startup never replaces an existing
   encryption key; TLS adoption retains the effective previous key. Keep the
   previous encryption password in a protected file. Do not put keys in command
   arguments, logs, Git, or shell history. Key and password files preserve
   spaces and other characters; only trailing LF line endings are removed,
   matching container startup. Use LF files, not CRLF.
3. Use the new release's WAR and Java 21 on a maintenance host/container with
   database access. Extract the WAR into a private working directory with
   `jar xf`. Set these environment variables to the deployment's actual
   connection and **absolute file paths**:

   ```sh
   export OE_UPGRADE_DB_URL=jdbc:postgresql://db.openelis.org:5432/clinlims
   export OE_UPGRADE_DB_USER=clinlims
   export OE_UPGRADE_DB_PASSWORD_FILE=/run/secrets/datasource.password
   export OE_UPGRADE_PREVIOUS_KEY_FILE=/run/secrets/previous-encryption-key
   export OE_UPGRADE_INSTALL_KEY_FILE=/etc/openelis-global/install/encryption.key
   java -cp 'WEB-INF/classes:WEB-INF/lib/*' org.openelisglobal.security.upgrade.EncryptedSecretsUpgrade
   ```

4. Require a successful exit before starting the new application with that same
   installation key. Verify a stored connection credential and result-access
   password can still be used. Remove the previous-key file from the runtime;
   retain it only with the protected pre-upgrade backup.

The standalone Liquibase changelog is deliberately excluded from application
startup. It records completion once in `DATABASECHANGELOG`. A shared schema
change first widens the two size-limited encrypted fields to text, so adding the
format marker cannot overflow existing values. Fresh installations apply that
same schema change without running key conversion. Work is paged by primary key
in batches of 250 and updates use primary keys. Completed batches survive
interruption; rerunning verifies and skips their `OE1:` values. A wrong key or
unreadable value fails the upgrade and rolls back its current batch; it is not
silently discarded. Blank values remain unchanged. Optional columns absent from
an older schema are skipped.

Never run this against a live application or an already-upgraded database with a
different destination key. An unmarked value must use the supplied previous key;
the upgrade does not guess which key encrypted it. To undo an upgrade, restore
the database and matching key/configuration backup together. This is a one-time
transition, not a general key-rotation tool.
