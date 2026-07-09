# HJRA Deployment Runbook -- Mindray HL7 Analyzers

> **Site:** HJRA (Madagascar) **Target date:** Week of 2026-03-28 **Analyzers:**
> Mindray BC-5380 (hematology), BS-200 (chemistry), BS-300 (chemistry)
> **Protocol:** HL7 v2.3.1 over MLLP (TCP)

---

## 1. MLLP Port Assignments

| Analyzer        | Category   | MLLP Port | Profile ID           |
| --------------- | ---------- | --------- | -------------------- |
| Mindray BC-5380 | HEMATOLOGY | 5380      | `hl7/mindray-bc5380` |
| Mindray BS-200  | CHEMISTRY  | 6001      | `hl7/mindray-bs200`  |
| Mindray BS-300  | CHEMISTRY  | 6002      | `hl7/mindray-bs300`  |

> BS-200 and BS-300 share the same HL7 interface but use distinct ports so both
> can run simultaneously.

---

## 2. Communication Model: HL7 PUSH

All three Mindray analyzers use **HL7 push** (not query/pull):

1. Analyzer completes a sample run
2. Analyzer opens an MLLP connection to OpenELIS (via bridge)
3. Analyzer sends `ORU^R01` message containing OBX results
4. OpenELIS parses results and replies with an `ACK` message
5. Connection closes

There is **no query workflow** in the current implementation -- OpenELIS does
not yet send `QRY` or `ORM` to these analyzers. The analyzer initiates all
communication. Bidirectional HL7 (ORM^O01 worklist download, QRY^Q02 order
download) is documented in vendor manuals but deferred to post-MVP. TCP
connectivity IS testable via the Admin UI "Test Connection" feature.

---

## 3. Network Path

```
Mindray Analyzer (Windows XP)
    |
    | TCP/MLLP on assigned port
    v
Linux VPN Gateway (site router)
    |
    | VPN tunnel
    v
OpenELIS Analyzer Bridge (bridge container)
    |
    | MLLP forwarding to OE HL7 listener
    v
OpenELIS Global (oe.openelis.org container, GenericHL7 plugin)
    |
    | Parse ORU^R01 -> persist results
    v
PostgreSQL (clinlims database)
```

### Bridge MLLP Forwarding

The bridge accepts inbound MLLP connections from the VPN and forwards them to
the OpenELIS HL7 listener. Configuration in `docker-compose.yml`:

```yaml
ports:
  - "5380:5380" # BC-5380
  - "6001:6001" # BS-200
  - "6002:6002" # BS-300
```

The bridge matches the inbound port to the registered analyzer and routes the
HL7 message to the correct GenericHL7 handler instance.

---

## 4. Test Catalog Mappings

### BC-5380 (13 tests -- hematology CBC)

| OBX Code | Test Name                                 | LOINC  | Unit    |
| -------- | ----------------------------------------- | ------ | ------- |
| WBC      | White Blood Cells                         | 6690-2 | 10^3/uL |
| RBC      | Red Blood Cells                           | 789-8  | 10^6/uL |
| HGB      | Hemoglobin                                | 718-7  | g/dL    |
| HCT      | Hematocrit                                | 4544-3 | %       |
| MCV      | Mean Corpuscular Volume                   | 787-2  | fL      |
| MCH      | Mean Corpuscular Hemoglobin               | 785-6  | pg      |
| MCHC     | Mean Corpuscular Hemoglobin Concentration | 786-4  | g/dL    |
| PLT      | Platelet Count                            | 777-3  | 10^3/uL |
| NEUT     | Neutrophils                               | 751-8  | %       |
| LYMPH    | Lymphocytes                               | 736-9  | %       |
| MONO     | Monocytes                                 | 742-7  | %       |
| EOS      | Eosinophils                               | 711-2  | %       |
| BASO     | Basophils                                 | 704-7  | %       |

### BS-200 and BS-300 (8 tests each -- chemistry panel)

| OBX Code | Test Name                  | LOINC  | Unit  |
| -------- | -------------------------- | ------ | ----- |
| GLU      | Glucose                    | 2345-7 | mg/dL |
| CREA     | Creatinine                 | 2160-0 | mg/dL |
| ALT      | Alanine Aminotransferase   | 1742-6 | U/L   |
| AST      | Aspartate Aminotransferase | 1920-8 | U/L   |
| ALB      | Albumin                    | 1751-7 | g/dL  |
| TP       | Total Protein              | 2885-2 | g/dL  |
| TBIL     | Total Bilirubin            | 1975-2 | mg/dL |
| UREA     | Blood Urea Nitrogen        | 3094-0 | mg/dL |

---

## 5. Pre-Deployment Checklist

- [ ] Current consolidation branch or `develop` contains the required HL7
      listener + analyzer-path implementation before deployment
- [ ] OpenELIS deployed with GenericHL7 plugin enabled
- [ ] Bridge container configured with MLLP port mappings (5380, 6001, 6002)
- [ ] VPN tunnel established between site and OpenELIS server
- [ ] Analyzer instances created via Admin UI or seed script with correct
      profiles
- [ ] Test catalog entries exist for all LOINC codes in the mappings above
- [ ] Firewall rules allow inbound MLLP on ports 5380, 6001, 6002

## 6. Validation Steps

1. **Connectivity test:** From the analyzer host (or VPN gateway), verify TCP
   connectivity:

   ```bash
   nc -zv <bridge-host> 5380
   nc -zv <bridge-host> 6001
   nc -zv <bridge-host> 6002
   ```

2. **Send test HL7 message:** Use the mock server or `mllp_send` to push a
   sample ORU^R01:

   ```bash
   mllp_send --host <bridge-host> --port 5380 < test-oru-bc5380.hl7
   ```

3. **Verify in OpenELIS:** Check Analyzer Results page for incoming results
   matching the test message sample ID.

4. **Confirm ACK:** The bridge/OE should return an `ACK^R01` with MSA-1 = `AA`
   (Application Accept).

## 7. Troubleshooting

| Symptom                                       | Likely Cause                               | Fix                                                            |
| --------------------------------------------- | ------------------------------------------ | -------------------------------------------------------------- |
| Connection refused on MLLP port               | Bridge not exposing port                   | Check `docker-compose.yml` port mapping                        |
| ORU received but no results                   | Test catalog mapping missing               | Verify analyzer profile `defaultConfigId` and LOINC entries    |
| ACK with `AE` (Application Error)             | Malformed HL7 or missing required segments | Check MSH, PID, OBR, OBX segment structure                     |
| "Unable to find test in test catalog" in logs | LOINC not in test catalog                  | Run seed script or add tests via Admin UI                      |
| Results appear as "unmapped"                  | `identifierPattern` mismatch               | Check MSH-3/MSH-4 against analyzer's `identifierPattern` regex |

---

## 8. Mindray-Specific Notes

- **MSH-3 (Sending Application):** Typically `MINDRAY` -- matched by
  `msh3_pattern` in the profile.
- **MLLP Framing:** Standard `0x0B` start, `0x1C 0x0D` end. No non-standard
  framing quirks known for these models.
- **Windows XP hosts:** The analyzers run on Windows XP. Ensure the VPN client
  is compatible. The analyzers' HL7 output settings must point to the VPN
  gateway IP and the correct MLLP port.
- **BS-200/BS-300 equivalence:** These two models share the same HL7 interface.
  The profiles are identical except for the `identifierPattern` and port. Site
  validation should confirm the OBX codes match the profile.
