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
