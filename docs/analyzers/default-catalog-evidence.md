# The analyzer harness dictionary

Bridge profiles define the instrument's codes, values and records, each with
standard codes. OpenELIS binds a profile's defaults by exact standard-code match
against its local catalog. The analyzer harness loads its own dictionary,
`projects/analyzer-harness/dictionary/`, holding only what the analyzer stories
use, so every shipped baseline profile binds every test, record and value on a
fresh setup with no operator work. It is test configuration, not the catalog a
site receives.

## How it loads

The `harness-catalog-init` service copies the dictionary into the writable
`configuration-data` volume before OpenELIS starts, keeping any file already
there (a Catalog Import upload, for example). OpenELIS loads it through its
ordinary configuration loader, domain by domain in load order. A domain the
dictionary supplies replaces OpenELIS's built-in files for that domain; every
other domain loads the built-in files, as in any deployment. Nothing else that
runs on the harness (its fixtures and specs) depends on a row only this
dictionary adds or changes.

The dictionary is `analyzer-harness-*.csv` files in the
`volume/configuration/backend/` shape:

- The tests the baseline profiles resolve by LOINC: HIV-1 Viral Load (20447-9),
  SARS-CoV-2 PCR (94500-6), Xpert MTB/RIF (85362-2) and Rifampin Resistance, as
  in the generic dictionary; Influenza A, Influenza B and RSV PCR (CDC LIVD
  codes 85477-8, 85478-6, 85479-4 for the Xpert Xpress CoV-2/Flu/RSV plus); an
  Internal Control DNA test (89578-3, QuantStudio). The Molecular Biology
  section and the Plasma and Serum sample types come from the base catalog; the
  dictionary adds Nasopharyngeal Swab and Sputum.
- A result component for every record a test reports (analyte calls, Ct and
  EndPt, internal controls, the HIV-1 call and log), and their answers.
- Answers carry standard codes: Positive LA6576-8, Negative LA6577-6, Invalid
  LA15841-2, Detected LA11882-0, Not detected LA11883-8, Pass LA10392-1 and Fail
  LA25389-0 (the answer list of 90101-7 "Internal control result"), with SNOMED
  and CIEL codes from the answer-terminology file; Indeterminate (SNOMED
  82334004, CIEL 1138) and Not applicable (SNOMED 385432009) have no LOINC
  answer code.
- Rifampin Resistance carries 89372-7 (presence of rifampicin resistance)
  instead of 46244-0, which identifies a mutation rather than reporting a yes/no
  result.
- COVIDPCR(Respiratory Swab) and COVIDPCR(Sputum), seeded by Liquibase in every
  deployment on 94500-6, are deactivated: they duplicate the generic SARS-CoV-2
  PCR test, and two usable tests on one code leave a profile's SARS-CoV-2
  unresolved. COVIDPCR(Fluid) is recoded to 94309-2 (specimen unspecified);
  94500-6 is for respiratory specimens only.

Xpert MTB/RIF and Rifampin Resistance are text results: the baseline profile
declares them without values until a vendor document gives them (roadmap rule
12). A rifampicin-resistance call reports resistance-associated mutations; it
must not be translated to resistant or susceptible, which come from growth-based
testing
([CDC](https://www.cdc.gov/tb/php/laboratory-information/xpert-mtb-rif-assay.html)).

## What proves it

`HarnessDictionaryIntegrationTest` loads the dictionary the way OpenELIS does at
startup and resolves the defaults of each baseline profile the Bridge ships;
every row binds. It also checks that no two active tests share an analyzer LOINC
on one specimen, and that the COVID report still finds its test. The harness E2E
then drives native instrument traffic through the Bridge to these tests.
