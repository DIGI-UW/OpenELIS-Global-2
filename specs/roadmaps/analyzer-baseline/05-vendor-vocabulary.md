# Step 5: Verify vendor vocabulary

Part of [analyzer-baseline-roadmap.md](../analyzer-baseline-roadmap.md). Read that file's Rules and Repo working agreements first; they apply here.

Goal: every code, value and record type a baseline profile declares is cited
from the vendor's own LIS or host-interface document.

### Facts

- Sources (rule 19): Cepheid's GeneXpert LIS Interface Protocol
  Specification (301-2002 Rev E) at
  `openelis-work/assets/vendor-manuals/genexpert-lis-protocol-spec.pdf`, the
  other vendor manuals beside it, and the per-instrument integration specs in
  `openelis-work/designs/analyzer-integration/`. Cepheid's assay LIS
  guidance is on its portal: 303-0251 Rev A (HIV-1 VL XC v3,
  `infomine.cepheid.com/sites/default/files/2023-05/303-0251 Rev. A LIS Guidance Xpert HIV-1 VL XC v3.pdf`)
  and 302-7279 Rev A (Xpress CoV-2/Flu/RSV plus,
  `infomine.cepheid.com/sites/default/files/2021-10/LIS Guidance Bulletin CoV-2 Flu RSV plus 302-7279 Rev. A.pdf`).
  The Confluence "Analyzer Integration Tracker" lists what exists per
  instrument.
- Verified in 301-2002 Rev E §6.3.4.1.6 (read 2026-10-05): R.3 component 4
  is the host test code, set per instrument in the Define Test Code dialog;
  5 the assay name (main results only); 6 the assay version; 7 the analyte or
  result name; 8 the complementary name (Ct, EndPt, Delta Ct, Conc/LOG).
  R.4 component 1 is the qualitative result, component 2 the quantitative.
  A quantitative assay uploads two main results, the second with `LOG` in
  R.3 component 8 and copies/mL in R.5. R.6 is `x to y`. R.7 flags: L, H,
  LL, HH, `<`, `>`, N, A, U, D, B, W. R.9: F, I, X (cannot be done; error in
  R.4 component 1), C. R.11 operator; R.12 and R.13 start and end; R.14
  computer name, instrument, module and cartridge serials, reagent lot,
  expiry. C records carry Notes or Error with code, description, details
  and time. H.2 delimiters are `@^\`. Examples: §6.3.4.1.9 to 6.3.4.1.11.
  Rev E §9.3.4.1 (the HL7 upload) says analyte results and their Ct, EndPt
  and Delta Ct are not uploaded, only Conc/LOG; the 2023 assay guidance below
  shows them in both ASTM and HL7 uploads, so a profile handles both.
- Verified in 303-0251 Rev A (read 2026-10-05): suggested host test code
  `HIVVL`; values DETECTED, NOT DETECTED, INVALID, ERROR, NO RESULT (§1).
  Examples per outcome (§2.1.1 ASTM, §2.1.2 HL7): `<40` is R.4 `DETECTED^`
  with R.7 `<` and R.6 `40.00 to 10000000.00`; a quantified result is R.4
  `^1009.64` with R.7 `N`; `>1x10^7` is `DETECTED^` with `>`; Not detected,
  INVALID and ERROR carry R.7 `A`, and ERROR adds a C record
  `Error^2097^Operation terminated^…`. Every outcome sends the LOG main
  result (R.3 component 8 `LOG`, range `1.60 to 7.00`), empty except when
  quantified (`^3.00`). Analyte records follow: HIV-1 (POS, NEG, NO RESULT,
  INVALID), IQS-H and IQS-L (PASS, FAIL, NO RESULT), each with Ct, EndPt and
  Delta Ct. HL7 carries the analyte and complementary name in OBX-4, the
  flag in OBX-8, the error in NTE. §3 is the translation table.
- Verified in 302-7279 Rev A: one product, three assay definitions
  (Xpress SARS-CoV-2_Flu_RSV plus, SARS-CoV-2_Flu plus, SARS-CoV-2 plus,
  §1); host test codes `SARSCOV2FLURSV`, `SARSCOV2FLU`, `SARSCOV2_3`; result
  codes SARSCOV2, FLUA, FLUB, RSV; values POSITIVE, NEGATIVE, ERROR,
  INVALID, NO RESULT; an assay host test code belongs to one assay definition
  (§2); suggested LOINC codes, 94500-6 for SARS-CoV-2 (§3); example messages
  use `COVFLURSVPLUS` as the panel code; translation table (§7).
- Order of work, not a limit (rule 13): the default GeneXpert profile
  declares every assay we can cite from Cepheid's LIS documents, starting
  with the current ones, Xpress CoV-2/Flu/RSV plus (302-7279, all three
  assay definitions), MTB/RIF Ultra and HIV-1 VL XC (303-0251). Older
  versions (original Xpress SARS-CoV-2 with PRESUMPTIVE POS, MTB/RIF G4,
  earlier HIV-1 VL) follow as their own assay definitions. A cartridge a lab
  runs that the profile does not declare is a per-analyzer row.
- Not verified: MTB/RIF Ultra wire codes and values (only package-insert
  display text seen: MTB DETECTED HIGH/MEDIUM/LOW/VERY LOW, MTB Trace
  DETECTED, RIF Resistance DETECTED/NOT DETECTED/INDETERMINATE, INVALID,
  ERROR, NO RESULT; site captures show result codes `MTB`, `MTB Trace`,
  `Rif Resistance`, which is a check of those sites' setup, not a source);
  which Dx software versions send analyte records.
- Standard codes: LOINC 85362-2 (MTBC DNA, sputum), 38379-4 (other specimen),
  89372-7 (rpoB presence), 20447-9 (HIV copies/mL), 29541-0 (log),
  94500-6 (SARS-CoV-2), 92131-2 (RSV). Each value lists every standard
  coding cited for it (rule 1). LOINC, read from the LOINC answer lists
  through the NLM LOINC service on 2026-10-05: Positive LA6576-8, Negative
  LA6577-6, Invalid LA15841-2 (LL2021-5, the example list of 94500-6 and
  85362-2); Detected LA11882-0, Not detected LA11883-8 (LL744-4, 89372-7);
  Inconclusive LA9663-1 (LL3250-9); none found for Indeterminate, Trace
  detected or the bacillary levels. SNOMED, from the CIEL release of 28 Apr
  2026: Detected 260373001, Not detected 260415000, Positive 10828004,
  Negative 260385009, Indeterminate 82334004. CIEL: 1301 Detected, 1302 Not
  detected, 703 Positive, 664 Negative, 163611 Invalid, 1138 Indeterminate;
  162202 / 170615 (TB PCR with RIF), 164942 (bacillary burden), 856 / 1305
  (HIV VL numeric / qualitative), 165840 (SARS-CoV-2 NAAT).
- Core set besides GeneXpert, under the same principle (agreed 5 Oct: all
  shipped profiles): FluoroCycler XT (`fluorocycler-xt-v4.json`, a `Plasma`
  specimen hint and no result type) and QuantStudio (`quantstudio-v3.json`),
  with their integration specs in `openelis-work/designs/analyzer-integration/`.
- Distro set to verify, from `openelis-madagascar-distro/configs/analyzer-profiles/`:
  ASTM Horiba Micros60, Pentra60, Mindray BA88A, Stago Start4, Sysmex XN;
  FILE DTPrime, Tecan F50, Wondfo, Multiskan, GeneXpert CSV, QuantStudio;
  HL7 Abbott Architect, GeneXpert HL7, Mindray BC2000, BC5380, BS200,
  BS300, BS360E. All untyped today.
- Evidence note format: one Markdown file per profile in the Bridge repo under
  `docs/profiles/<profile-id>.md`: document title, revision, URL or archived
  path; a table of code, value or record type to the page or section that
  defines it; a table of where each result part sits (rule 17); the value
  translations (rule 18); an "unverified" list.

### Build

```
- [ ] T5.1 GeneXpert: write docs/profiles/genexpert-astm.md from 301-2002 Rev E and the assay guidance (303-0251 HIV-1 VL XC, 302-7279 Xpress); locate MTB/RIF Ultra guidance (openelis-work first); cover the three assays in scope
- [ ] T5.2 FluoroCycler XT and QuantStudio, then each distro profile, in the order core will ship them: locate the vendor host-interface document (openelis-work vendor-manuals and integration specs first); write docs/profiles/<id>.md; mark unverifiable rows
```

### Verify

```bash
ls docs/profiles/            # one file per profile entering step 6
grep -c "^| " docs/profiles/genexpert-astm.md   # every declared row cited
```

### Done when

1. Every profile entering step 6 has an evidence note citing the vendor
   document for every code, value and record type it declares. (read)
2. GeneXpert MTB/RIF Ultra is cited from Cepheid LIS guidance, not
   package-insert display text; software-version and language dependencies
   are stated.
   (read)
3. Unverifiable items are listed and left out of the profile. (read)

### Background (optional)

- [P2](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#p2), [P4](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#p4), [Proper concepts](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#concepts), [Not verified](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#limits)
