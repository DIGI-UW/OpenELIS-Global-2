# M2 Validation Report (feat/013-ogc-327-bc5380-hl7-m2-profile-match)

**Date**: 2026-03-10  
**Branch**: `feat/013-ogc-327-bc5380-hl7-m2-profile-match`  
**Reference**: [hl7-readiness-gates.md](../contracts/hl7-readiness-gates.md),
[m2-ogc327-bc5380-tasks.md](../milestone-outlines/m2-ogc327-bc5380-tasks.md)

## Summary

| Check                         | Status          |
| ----------------------------- | --------------- |
| Main project build            | PASS            |
| HL7-related tests (main repo) | PASS (17 tests) |
| Gate 1 evidence (M1)          | Inherited       |
| BC-5380 HL7 lane              | PASS            |

## Automated Tests (Main Repo)

| Test Class                        | Tests | Result |
| --------------------------------- | ----- | ------ |
| `AnalyzerImportControllerHL7Test` | 3     | PASS   |
| `HL7AnalyzerReaderTest`           | 4     | PASS   |
| `HL7MessageServiceTest`           | 9     | PASS   |
| `HL7MessageOutServiceTest`        | 1     | PASS   |

### AnalyzerImportControllerHL7Test Coverage

- `postHl7_validOruR01_reachesEndpointAndParses` — Valid BC-5380 HL7 fixture
  reaches endpoint, parses, does not return 400
- `postHl7_invalidMessage_returnsBadRequest` — Invalid payload returns 400
- `postHl7_emptyBody_returnsBadRequest` — Empty body returns 400

**Note**: Full ingestion (200) is not asserted in main-repo tests because
GenericHL7 plugin is loaded from JARs at runtime; test environment has no plugin
JARs. BC-5380 full ingestion is covered by `GenericHL7IntegrationTest` in the
plugins module.

## Bridge & Plugins (Not Run in This Validation)

- **Bridge** (`tools/openelis-analyzer-bridge`): Has `astm-http-lib` dependency
  not in Maven Central; build requires parent repo or multi-module setup. Gate 1
  evidence checklist states bridge tests are in `HapiMLLPListenerTest` and
  `UnifiedRoutingTest` — these run in CI when bridge is built.
- **GenericHL7 plugin**: Tests live in `plugins/analyzers/GenericHL7/`; main
  project excludes plugins from default build. `GenericHL7IntegrationTest`
  validates full BC-5380 ingestion with mocked plugin list.

## Gate 2 Evidence (BC-5380)

| Requirement                | Evidence                                                                                                                             |
| -------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| Gate 1 passed              | Inherited from M1 (PR #3033)                                                                                                         |
| BC-5380 in HL7 lane        | `madagascar-analyzer-test-data.xml` analyzer 2007: `protocol_version="HL7_V2_3_1"`, `identifier_pattern="MINDRAY.*BC.?5380\|BC5380"` |
| BC-5380 uses listener path | Harness `docker-compose.analyzer-test.yml`: MLLP 2575, `ORG_ITECH_AHB_MLLP_ENABLED=true`, forwards to `/analyzer`                    |
| Profile seed               | `projects/analyzer-profiles/hl7/mindray-bc5380.json` exists                                                                          |
| HL7 fixture                | `testdata/hl7/mindray/bc5380-cbc-result.hl7` — MSH-3 "MINDRAY BC-5380" matches pattern                                               |

## Architecture Compliance

- **HL7Plugin-only**: No fallback in `HL7AnalyzerReader`; plugin matching only
- **No ASTM contamination**: BC-5380 is HL7-only in fixtures and profile

## Limitations

1. **Bridge tests**: Not run locally (missing `astm-http-lib`); rely on CI
2. **GenericHL7 plugin tests**: In plugins module; run with `with-plugins`
   profile or from plugins submodule
3. **Docker E2E**: Not run; harness config is ready for manual verification
