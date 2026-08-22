# Analyzer Profile Source Corpus

These JSON files preserve the pre-catalog profile source corpus for curation and
provenance. They are not an OpenELIS runtime catalog, a test profile root, or a
source that OpenELIS may serve to Analyzer Bridge.

The authoritative runtime catalog is owned by Analyzer Bridge. OpenELIS stores
an exact profile ID/revision pin and local site bindings; the analyzer harness
resolves published revisions through the Bridge-backed catalog contract. Do not
add a runtime consumer for this directory or copy these files into an OpenELIS
profile authority.

The established profile contract still has exactly two jobs: define
communication/runtime behavior for one analyzer type and supply defaults when
OpenELIS creates an instance of that type. OGC-1054 evolves that contract with
strict validation, immutable revisions, and Bridge catalog lifecycle; it does
not introduce another profile model.

## Directory layout

```
projects/analyzer-profiles/
├── astm/   — GenericASTM profiles (TCP/IP ASTM LIS2-A2)
├── hl7/    — GenericHL7 profiles (TCP/IP HL7 v2.x over MLLP)
└── file/   — GenericFile profiles (filesystem CSV / Excel / ODS drops)
```

## Curation

Each source file is retained, corrected, represented as a proven alias, split,
or removed according to instrument evidence. It is never published merely
because it exists here. Priority profiles are published from the validated
Bridge catalog only after their contract, Bridge runtime behavior, and
analyzer-mock traffic pass together.
