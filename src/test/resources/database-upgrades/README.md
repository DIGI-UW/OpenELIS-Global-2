Existing-data upgrade checks run in `DatabaseUpgradeIntegrationTest` as part of
the backend CI gate. They use disposable PostgreSQL databases and the complete
application changelog with deployment contexts (`default`), without Spring
initializing and migrating the schema before fixture data is loaded.

Each entry in `index.txt` names a properties file specifying:

- `before.id` and `before.file`: the first pending changeset at the upgrade
  boundary.
- `data`: a Liquibase fixture loaded before that boundary.
- `assertions`: Liquibase preconditions verifying preserved data and resulting
  schema.
- `contexts`: deployment contexts, defaulting to `default`.
- Optional `expected.failure`: text identifying an intentionally blocked unsafe
  upgrade.

The runner applies all preceding pending changes, loads existing data, then runs
the entire remaining candidate changelog. Successful upgrades must be
repeatable. An expected failure must preserve the data asserted by its fixture.

The analyzer fixtures cover a pending-registration draft, an intentionally
inactive record, a migrated active analyzer with a real clinical binding, and
retained unmigrated configuration. These checks cover schema upgrades; they do
not claim to create or verify an external Bridge connection. Configured
analyzers require the documented Bridge handoff before destructive cleanup.

Run just these checks with Java 21 and Docker:

```sh
mvn -B -ntp -Dtest=DatabaseUpgradeIntegrationTest test
```

For future migrations, add a fixture at the relevant boundary instead of adding
another migration-specific runner. This reconstructs a prior schema from the
checked-in changelog and bootstrap; it is not a restore of a live database and
does not replace validation against supported release backups.
