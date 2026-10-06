# Shipped analyzer clinical defaults

Bridge profiles define the instrument codes and runtime behavior; OpenELIS
resolves those codes to its local clinical catalog. The core configuration in
`src/main/resources/configuration/` ships the analyzer result options, for
example `dictionaries/analyzer-result-options.csv`. The analyzer harness adds
its molecular tests and their result choices from
`projects/analyzer-harness/config-templates/`, loaded through OpenELIS's normal
configuration loader by the `harness-catalog-init` service. Those harness CSVs
are test configuration, not part of the catalog every site receives.

## Xpert rifampin-resistance outcomes

The harness `Xpert RIF Resistance` test uses the
`Analyzer GeneXpert RIF Results` choices `DETECTED`, `NOT DETECTED` and
`Indeterminate`. The GeneXpert profile reports the raw values `DETECTED`,
`NOT DETECTED` and `INDETERMINATE`; the generic resolver handles the difference
in capitalization.

These are molecular resistance-detection outcomes. They must not be translated
by a global detected-to-resistant or not-detected-to-susceptible rule. The
[Cepheid instructions, 303-0942 Rev. B, Results](https://web-support.cepheid.com/Package%20Insert%20Files/Xpert%20MTB-RIF/Xpert%20MTB-RIF%20ENGLISH%20IFU%20303-0942%20Rev%20B.pdf)
describe presence, absence or an indeterminate call for resistance-associated
mutations.
[CDC interpretation guidance](https://www.cdc.gov/tb/php/laboratory-information/xpert-mtb-rif-assay.html)
also distinguishes these calls from growth-based susceptibility testing.

The detected and not-detected choices have their own dictionary identities.
Previously stored Resistant/Susceptible choices and historical clinical results
are not renamed or deleted, and existing local bindings are not silently
rewritten. Site-uploaded catalog overrides remain site-owned.

`AnalyzerCatalogIdentityIntegrationTest` exercises the production catalog
handlers and default resolver, including repeat loading with stable option IDs.
The GeneXpert harness workflow sends native ASTM messages for each outcome and
checks the saved value, patient, order, test and specimen.

## COVID answer wording and specimen selection

The respiratory COVID test already has `SARS-CoV-2 RNA DETECTED` and
`SARS-COV-2 RNA NOT DETECTED` choices, so adding a Positive/Negative pair would
duplicate clinical answers. Instead, the profile contract supplies optional
`result_value_hints`, keyed by exact reported values. Resolution uses an exact
raw label first, then the hinted equivalent label if no exact match exists.
Ambiguous matches stay unresolved, and the raw value is kept.

GeneXpert profile revision 7 (Bridge 3.2.4) supplies these hints together with a
`Respiratory Swab` specimen hint. The specimen hint is needed because the
catalog also contains COVID tests for other specimens.

The
[Cepheid instructions, 302-3562 Rev G, section 16](https://www.cepheid.com/Package%20Insert%20Files/Xpress-SARS-CoV-2/Xpert%20Xpress%20SARS-CoV-2%20Assay%20ENGLISH%20Package%20Insert%20302-3562%20Rev.%20G.pdf)
support the positive/negative detection meanings and distinguish INVALID, ERROR
and NO RESULT. The profile does not equate ERROR with Invalid or invent an
INDETERMINATE translation.

The test's primary result is that categorical overall call. Its only numeric
components are the non-primary N2 and E cycle-threshold values that
`055-seed-molecular-components.xml` adds with the `Ct` unit, which stay blank
when an instrument does not transmit per-target Ct.

What remains to be qualified is tracked in the
[analyzer baseline roadmap](https://github.com/DIGI-UW/OpenELIS-Global-2/blob/develop/specs/roadmaps/analyzer-baseline-roadmap.md).
