# Shipped analyzer clinical defaults

Core analyzer tests load the application catalog from
`src/main/resources/configuration/`, following normal database migrations. They
do not install a separate test catalog or repair analyzer mappings. Bridge
profiles define the instrument codes and runtime behavior; OpenELIS resolves
those codes to its local clinical catalog.

## Xpert rifampin-resistance outcomes

The supplied `Xpert RIF Resistance` test uses `DETECTED`, `NOT DETECTED` and
`Indeterminate`. The pinned Bridge profile reports the corresponding raw values
`DETECTED`, `NOT DETECTED` and `INDETERMINATE`; the generic resolver already
handles capitalization differences.

These are molecular resistance-detection outcomes. They must not be translated
by a global detected-to-resistant or not-detected-to-susceptible rule. The
[Cepheid instructions, 303-0942 Rev. B, Results](https://web-support.cepheid.com/Package%20Insert%20Files/Xpert%20MTB-RIF/Xpert%20MTB-RIF%20ENGLISH%20IFU%20303-0942%20Rev%20B.pdf)
describe presence, absence or an indeterminate call for resistance-associated
mutations.
[CDC interpretation guidance](https://www.cdc.gov/tb/php/laboratory-information/xpert-mtb-rif-assay.html)
also distinguishes these calls from growth-based susceptibility testing.

The catalog correction uses new dictionary identities for the detected and
not-detected choices. It does not rename or delete previously stored
Resistant/Susceptible choices or historical clinical results. Existing local
bindings are not silently rewritten. The normal loader installs the new options
where bundled configuration is in use; site-uploaded overrides remain
site-owned.

`AnalyzerCatalogIdentityIntegrationTest` exercises the production catalog
handlers and default resolver, including repeat loading with stable option IDs.
The parameterized GeneXpert Playwright workflow sends synthetic native ASTM
messages and checks exact saved values, patient, order, test and specimen. These
outcome checks do not establish full assay, hardware or release qualification;
see the
[remediation plan](../../specs/roadmaps/analyzer-test-remediation-plan.md).

## COVID answer wording and specimen selection

The original respiratory COVID test already has `SARS-CoV-2 RNA DETECTED` and
`SARS-COV-2 RNA NOT DETECTED` choices. Adding another Positive/Negative pair
would duplicate clinical answers. The shared profile contract instead supplies
optional `result_value_hints`, keyed by exact reported values. Generic
resolution uses an exact raw label first, then the explicit equivalent label if
no exact match exists. Ambiguous matches stay unresolved; the raw value is
unchanged.

[Bridge draft #69](https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/69)
supplies these hints and `Respiratory Swab` context in candidate GeneXpert
revision 7. The specimen context is necessary because the migrated catalog also
contains COVID tests for other specimens. The real-catalog test exposed this
additional ambiguity; correcting duplicate creation alone did not resolve it.

The
[Cepheid instructions, 302-3562 Rev G, section 16](https://www.cepheid.com/Package%20Insert%20Files/Xpress-SARS-CoV-2/Xpert%20Xpress%20SARS-CoV-2%20Assay%20ENGLISH%20Package%20Insert%20302-3562%20Rev.%20G.pdf)
support positive/negative detection meanings and distinguish INVALID, ERROR and
NO RESULT. The candidate does not equate ERROR with Invalid or invent an
INDETERMINATE translation. Complete vocabulary/capture qualification and held
result recovery remain open. The existing editor can author the hints; an older
Bridge schema will reject them until the linked contract is deployed.

Focused tests cover unique/ambiguous/exact matching, retained raw values, the
original respiratory clinical IDs and answers, and editor persistence. This
checkpoint does not establish native-traffic qualification or authorize a new
release pin; the candidate must still pass the complete protocol workflows.

## Mixed-component result display checkpoint

The candidate native COVID run exposed a separate display failure after correct
mapping and delivery. The existing COVID clinical test contains categorical
answers and numeric Ct components. `AnalyzerResultsController` looked up every
active result definition as a dictionary entry, including numeric definitions
with no dictionary value. The resulting exception returned an empty worklist.

The display lookup now uses the existing component option service when a
component is known and resolves only dictionary-valued definitions. No catalog
or mapping repair is performed. After rebuilding, both original pending COVID
observations were visible with their original raw values and correct answer
choices, without resending or editing them.

Validation on the local candidate: the three existing controller tests and three
real-catalog tests passed. A recorded native run passed authentication, MTB NOT
DETECTED, COVID POSITIVE and COVID NEGATIVE. Its clinical readback asserts one
ordered analysis, one persisted observation in the catalog's primary component,
the exact answer and empty unreported components. Extra empty Ct fields are not
duplicate saved results. These scenarios use candidate Bridge #69 revision 7;
they do not qualify all profile rows or authorize a release pin.

The browser still reports an aborted empty HTTP 200 response for some Save
requests. Saved-result assertions pass. The exact browser cancellation cause
remains unverified; this diagnostic alone does not establish a failed save.

Additional follow-ups retained from the same runtime inspection:

- The COVID primary component metadata declares numeric type even though its
  answer definitions are categorical. Review the normal component/catalog
  initialization separately; do not rewrite existing results to repair display.
- In the local harness, connection/profile data uses the image's persistent
  `/data/openelis-analyzer-bridge` volume, but the delivery outbox and FILE
  state default to `/tmp/openelis-analyzer-bridge`. Configure persistent
  database paths before claiming container-replacement/replay acceptance. A
  simple process restart is not sufficient evidence for that requirement.
- Two-instrument attribution, HIV numeric traffic, complete FILE/HL7 workflows,
  recovery, populated upgrades and full profile compatibility remain open.
