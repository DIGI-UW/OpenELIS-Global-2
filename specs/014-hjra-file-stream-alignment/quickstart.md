# Quickstart: FILE Analyzer Stream Alignment

## Runtime Ownership

The bridge is the only runtime owner of directory watching, polling, file
stability detection, retries, and transport. OpenELIS configures FILE
analyzers, synchronizes bridge registration, receives authenticated direct
submissions, and processes results/QC.

No OpenELIS FILE watcher or poller exists in this feature. Any future fallback
requires a separate specification and must be disabled by default.

## Configure a FILE Analyzer

1. Start from `/analyzers/types?protocol=FILE`.
2. Inspect a shipped profile from `projects/analyzer-profiles/file/`.
3. Select **Set up** and complete the Instrument, Verify, Connect, and Review
   steps.
4. Configure protocol-appropriate file format and directory fields.
5. Verify test/result mappings and profile-applicable QC.
6. Save and confirm that OpenELIS synchronizes the analyzer registration to the
   bridge.

The selected profile is copied once through `defaultConfigId`. Analyzer-specific
overrides then live with the analyzer configuration; the shipped profile is not
edited or live-linked.

## Add or Amend a Profile

1. Obtain a representative vendor export.
2. Confirm that the generic FILE format/plugin contract can parse it.
3. Add or amend profile JSON under `projects/analyzer-profiles/file/`.
4. Add fixtures that prove format normalization and mapping behavior.
5. Validate the profile schema and OpenELIS registration contract.
6. Validate bridge watch/delivery behavior in bridge or harness tests.

Do not add an instrument-specific OpenELIS watcher or parser when the profile
and generic plugin can express the format. Record an explicit blocker when
representative vendor evidence is unavailable.

## Validation

OpenELIS owns:

```bash
mvn -Dtest=org.openelisglobal.analyzer.** test
mvn spotless:check
```

The bridge/harness owns watch registration, update/removal, file stability,
delivery, retry, and dead-letter tests. UI E2E owns only the visible
configuration and review workflow; it does not inspect bridge payload internals
or simulate a file poller.

## Key Paths

| Concern | Path |
| --- | --- |
| Governing ownership | `AGENTS.md` |
| Normative feature spec | `specs/014-hjra-file-stream-alignment/spec.md` |
| FILE profiles | `projects/analyzer-profiles/file/` |
| Profile schema | `projects/analyzer-profiles/schema/analyzer-defaults-1.0.schema.json` |
| Analyzer QC/config setup | `specs/OGC-1054-analyzer-qc-config/spec.md` |
| Bridge implementation | `tools/openelis-analyzer-bridge/` |

Production result import, multi-component mapping, and Results/Validation v4
acceptance remain separate milestones.
