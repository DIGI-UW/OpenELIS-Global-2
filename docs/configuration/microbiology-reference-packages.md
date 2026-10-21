# Microbiology reference packages

OpenELIS loads independently released AMR reference ZIPs through the existing
configuration loader. Reference data is owned by the AMR exporter, not bundled
in OpenELIS or inserted by Liquibase. No R runtime is needed in OpenELIS.

The companion exporter is
[DIGI-UW/AMR](https://github.com/DIGI-UW/AMR/tree/c/ogc-1533-reference-package/data-raw/openelis).
The supported contract is `openelis-amr-reference/v1`. A deployment pins the
published archive URL, package version, and SHA-256 separately from its
application image digest. It verifies the archive before placing it in:

```
<org.openelisglobal.configuration.dir>/microbiology-reference-packages/*.zip
```

The default configuration root is
`/var/lib/openelis-global/configuration/backend`. Startup loads packages when
`org.openelisglobal.configuration.autocreate=true`. Without a package, existing
catalogs and manual imports continue to work; OpenELIS does not invent a
reference catalog. An older application without this handler does not import
these ZIPs. A deployment requiring AMR metadata must pin a compatible
application as well as the package and verify the configuration import result,
not just HTTP health.

## Archive contract

The ZIP has exactly nine root entries: `manifest.json`, `scope.json`,
`organisms.csv`, `antimicrobials.csv`, `breakpoints.csv`,
`interpretation-vectors.csv`, `coverage.csv`, `source-rows.csv`, and
`NOTICE.txt`. Both compressed and expanded payloads are limited to 32 MiB. The
manifest declares its format, dotted numeric package version,
`amr-reference-<version>` identity, 40-character source commit, scope, editions,
and every payload's SHA-256, byte length and CSV row count. The distro's archive
checksum is the trust pin; internal checksums detect inconsistency, not
publisher authenticity.

Organisms and antimicrobials use stable `(source, code)` identities in the `AMR`
namespace. Scope must agree with the manifest and concept inventory. Each
edition has publisher `EUCAST`, guideline `EUCAST <year>`, and version
`<year>-<package-version>`. The first exporter scope is **2025 and 2026**, with
MIC only. The loader does not select a rolling latest year.

Supported rules target explicit organisms and antimicrobials, use MIC in mg/L,
and preserve `S <= susceptible_value` and `R > resistant_value`. Values between
these limits are intermediate. The database retains the strict resistant
boundary; it is not rounded to a subsequent twofold dilution. Existing manual
rules retain their inclusive resistance behavior. Contextual/site-specific
rules, no-susceptible rules and thresholds outside receiving precision are
rejected by this contract.

Every rule must be marked supported in coverage, every excluded context must
have a reason and no rule, and coverage must account for its source rows.
Source-generated interpretation vectors are evaluated against the actual
persisted rules by the OpenELIS interpreter, including equality and values
either side of the thresholds. Any disagreement fails the whole package
transaction.

## Identity, upgrades and laboratory control

Concepts are matched by source identity, or adopted by an unambiguous existing
name/WHONET code. Conflicting matches fail. Adoption and reload preserve
existing IDs, names, activity and clinical settings. New concepts are created
through the application service. The migration only adds provenance fields and
boundary semantics; it contains no concepts.

Each standard retains the archive hash. Reloading the same archive is
idempotent; changed bytes cannot replace an existing edition. New package
versions create new, inactive editions. Keep downloaded archives in deployment
storage for provenance and recovery. Archive removal does not delete database
history.

Package-managed rules cannot be edited through administration, CSV upload, or
scenario configuration helpers. Administration explains why they are read-only.
Activation and archiving remain explicit laboratory actions. New loading does
not change the active edition. Existing results retain their recorded standard.
Schema rollback refuses to discard imported identities, package provenance or
strict resistance boundaries; restore a pre-import backup when rolling back such
an installation.

## Validation

Synthetic contract tests run with the normal backend suite. To also load a
generated candidate through the real configuration handler and evaluate its
source vectors:

```sh
mvn test -Dtest=MicroReferencePackageIntegrationTest \
  -Damr.reference.artifact=/absolute/path/amr-reference-2026.1.zip
```

This contract check uses an isolated Testcontainers database. It does not
replace validation of the deployed PNG test panels, instrument mappings,
technician workflows, or clinical coverage. The initial exporter deliberately
omits unsupported contexts and disk diffusion; a concept's presence does not
imply it has supported breakpoints.
