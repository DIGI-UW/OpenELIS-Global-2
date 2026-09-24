# Analyzer connection migration

This standalone Java 21 tool performs the one-time M3 cutover from released
OpenELIS analyzer connection fields to durable Bridge connections. It is not
included in the OpenELIS WAR.

Run it only while analyzer configuration and traffic are quiesced:

1. Export `GET /rest/analyzer/migration/source` from the pre-removal OpenELIS
   build as a global administrator.
2. Author one explicit selection per source analyzer. A selection contains
   `method: EXPLICIT`, the exact Bridge `profileRef`, `selectedBy`,
   `selectedAt`, and optional generic `connectionValues`. An analyzer may
   instead use `method: EXCLUDE` with a non-empty `reasonCode`.
3. Run `plan` and correct every `NEEDS_CORRECTION` outcome.
4. Run `apply` against the deployed Bridge and OpenELIS migration endpoints.
5. Restart Bridge, restore any approved active connection through the normal
   OpenELIS activation workflow, then run `verify`.
6. Retain the source, selections, and all three manifests as cutover evidence.
   Remove the temporary OpenELIS migration endpoints and released runtime fields
   before the final application candidate.

Build:

```bash
mvn package
```

The executable is `target/analyzer-connection-migration-1.0.0.jar`. Each mode
requires `--source`, `--output`, and `--run-id`.

PLAN also requires `--selections` and `--profiles`:

```bash
java -jar target/analyzer-connection-migration-1.0.0.jar plan \
  --source source.json \
  --selections selections.json \
  --profiles ../openelis-analyzer-bridge/src/main/resources/analyzer-profiles \
  --output plan.json \
  --run-id analyzer-cutover-plan-1
```

APPLY requires the same files plus `--bridge-url`, `--openelis-url`, and an
`--openelis-cookie-file` containing the global administrator session cookie.
VERIFY replaces `--selections` and `--profiles` with `--apply-manifest` and uses
the same endpoint and cookie options.
