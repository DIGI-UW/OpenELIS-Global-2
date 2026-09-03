# CPHL EQA cutover checklist — Access/Excel to OpenELIS

**Scope.** OGC-935 (PNG Phase II, contract item 18). This checklist confirms
that OpenELIS replaces the two systems CPHL Port Moresby uses to run its
national EQA programme: the Access database that produces performance reports,
and the Excel workbooks that track sample distribution to provincial
laboratories. Cutover is complete when every row in §2 and §3 is checked and a
CPHL stakeholder signs §6.

**How to use this document.** Work through it on a CPHL instance with the
programme registry loaded (§4). Each unchecked row is either a task or a
question for CPHL — there are no other states.

---

## 1. Programme registry (confirm with CPHL)

The registry file (`docs/eqa/cphl-eqa-programs.csv`, loaded per §4) defines six
provider-side schemes, one per ePT-validated test domain (AC-V2.1-23). The
domains are the best available registry; CPHL confirms or corrects each row at
sign-off. Every value is editable afterwards on **Schemes & Programs**.

| Programme                            | Test section      | Frequency | CPHL confirms                |
| ------------------------------------ | ----------------- | --------- | ---------------------------- |
| CPHL National HIV Serology EQA       | Serology          | Quarterly | ☐ name ☐ section ☐ frequency |
| CPHL National HIV Viral Load EQA     | Molecular Biology | Quarterly | ☐ name ☐ section ☐ frequency |
| CPHL National EID EQA                | Molecular Biology | Quarterly | ☐ name ☐ section ☐ frequency |
| CPHL National HIV Recency EQA        | Serology          | Quarterly | ☐ name ☐ section ☐ frequency |
| CPHL National COVID-19 Molecular EQA | Molecular Biology | Quarterly | ☐ name ☐ section ☐ frequency |
| CPHL National TB Microscopy EQA      | Microbiology      | Quarterly | ☐ name ☐ section ☐ frequency |

Open questions for CPHL:

- ☐ Are there programmes CPHL runs that are missing from this list (for example
  syphilis, malaria microscopy, CD4)?
- ☐ Which international or regional schemes does CPHL _participate in_ (as
  opposed to provide)? Those are entered as `INTERNATIONAL_PT` schemes with the
  external provider named — none are seeded, because no authoritative list
  exists in the project record.
- ☐ The provincial laboratory register (name, province, contact, delivery
  address, FHIR endpoint if any) — entered as Organizations, then enrolled per
  scheme on the **Participants** page.

## 2. Excel sample distribution → OpenELIS

Each row maps one thing the Excel workbooks do today to the screen that replaces
it. Verify each by doing it once on the seeded instance.

| Excel artifact today                                            | OpenELIS replacement                                                                                                                 | FRS        | Verified |
| --------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ | ---------- | -------- |
| Round planning sheet (which labs get which panel, when)         | Cycle wizard: cycle details, panel samples, participant roster, distribution method (**Provider Cycles → Create new cycle + panel**) | FR-V2.5-02 | ☐        |
| Aliquot count arithmetic (needed vs produced, reserve)          | Prep workbench: aliquots produced vs needed gauge, homogeneity QC gate                                                               | FR-V2.5-12 | ☐        |
| Dispatch register (courier, tracking number, date sent per lab) | Shipment workbench: per-participant box, courier details, mark shipped                                                               | FR-V2.5-13 | ☐        |
| Receipt tracking (phone/email follow-up, arrival dates)         | Receipt monitor: delivery status, overdue rule, temperature-excursion flag                                                           | FR-V2.5-14 | ☐        |
| Replacement sample notes (damaged/lost panels)                  | Reprovision action ("Send repeat"): reserve decrement, linked repeat shipment                                                        | FR-V2.5-15 | ☐        |
| Results collection workbook (per-lab result entry)              | Receipt monitor → **Enter results**: keyed per participant (numbers or words such as Reactive) or the participant's export-bundle CSV pasted in; results from a participant OpenELIS arrive through the provider's FHIR store on their own | FR-V2.5-03 | ☐        |

## 3. Access reporting → OpenELIS

| Access artifact today                          | OpenELIS replacement                                                                                             | FRS           | Verified |
| ---------------------------------------------- | ---------------------------------------------------------------------------------------------------------------- | ------------- | -------- |
| Per-lab performance report (scores, pass/fail) | Printed CPHL-format performance report: section- and programme-level summaries, z-score table, cycle identifiers | OGC-933       | ☐        |
| Standard interpretive commentary               | Interpretive comments on the report (pre-approved picker)                                                        | OGC-934       | ☐        |
| Programme-level summary across labs            | Provider scoring view + participant performance dashboard                                                        | FR-V2.5-04/05 | ☐        |
| Chase list of poor performers                  | Follow-up register: unacceptable-score auto-enqueue, triage actions                                              | FR-V2.5-06/07 | ☐        |

## 4. Loading the programme registry

The registry is a CSV config file, not code — edit the file, not the database.
On the CPHL instance, copy `docs/eqa/cphl-eqa-programs.csv` into the backend
configuration directory:

```
/var/lib/openelis-global/configuration/backend/eqa-programs/cphl-eqa-programs.csv
```

Restart the webapp, or call `POST /rest/configuration/domains/reload` to load it
without a restart. The loader runs the file only when its checksum changes, and
rows upsert by programme name: a new name inserts, an existing name updates the
columns the file names — so correcting the CSV and reloading is the whole
maintenance workflow. A `testSection` value missing from the instance's catalog
leaves that programme's section blank instead of failing the file (assign it on
**Schemes & Programs**).

Verify with:

```sql
SELECT name, provider, scheme_type, frequency FROM clinlims.eqa_program
WHERE provider LIKE 'Central Public Health Laboratory%';
```

Six rows are expected.

**Visibility note:** the provider scheme board lists a scheme only after its
first active participant enrollment, matching the cycle wizard's own
precondition (a cycle needs enrolled labs). Directly after loading, the six
programmes appear on the **Participants** page's programme selector — enroll the
provincial labs there first (§1), and each scheme joins the board as its first
lab enrolls.

## 5. Data migration decision

Historical Access/Excel records are **not** migrated. Past cycles stay in Access
as an archive; OpenELIS starts with the first live cycle after cutover. If CPHL
requires historical scores inside OpenELIS, that is new scope to raise on
OGC-935 — record the decision here: ☐ archive stands / ☐ migration requested.

## 6. Sign-off

Run one seeded scheme's cycle end-to-end (wizard → prep → ship → receipt →
results → scoring → printed report) with CPHL staff driving.

| Role                    | Name | Date | Signature |
| ----------------------- | ---- | ---- | --------- |
| CPHL EQA coordinator    |      |      |           |
| CPHL laboratory manager |      |      |           |
| Implementation team     |      |      |           |
