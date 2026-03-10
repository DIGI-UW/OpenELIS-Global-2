# Pre-M1 Readiness Checklist

**Purpose**: Ensure the stream can launch M1 when teams are ready.  
**Reference**: [quickstart.md](../quickstart.md),
[paired-pr-handoff.md](../contracts/paired-pr-handoff.md)

## Submodule and Branch Sync

- [ ] `develop` is up to date with the primary remote
      (`git pull --rebase origin develop` in this repo; substitute another
      remote name if needed)
- [ ] Submodules initialized and updated
      (`git submodule update --init --recursive`)
- [ ] Required submodule versions or pins documented below (if any intentional
      pins)

### Current observed submodule commits on `spec/013-hjra-hl7-stream-alignment`

These are the commits observed while preparing the spec branch. Revalidate them
against `develop` again immediately before opening M1.

| Submodule                      | Observed Commit                            | Notes                         |
| ------------------------------ | ------------------------------------------ | ----------------------------- |
| tools/openelis-analyzer-bridge | `6200254fe2779a88dfeed778df963fa05f419aa0` | Bridge MLLP work for OGC-325  |
| plugins                        | `c5e24819cfbdcab301cef2060de2b9c39035bf80` | GenericHL7-related fixes      |
| tools/analyzer-mock-server     | `4c8f1aacc8e1bf56ee505f40290bde06b64a8507` | E2E mock with profile support |

_If M1 intentionally pins any of these to different commits, record the
exception and rationale here before branch creation._

## Team Agreement

- [ ] Bridge and main-repository teams have agreed on paired PR model per
      `contracts/paired-pr-handoff.md`
- [ ] Evidence expectations (Gate 1) are understood: MLLP listener, ACK, routing
      to `/analyzer/hl7`, representative ingestion path, mock-with-profile E2E
      proof

\_Sign-off or acknowledgment: \***\*\*\*\*\*\*\***\_\***\*\*\*\*\*\*\*** (date:
**\_\_**)\_
