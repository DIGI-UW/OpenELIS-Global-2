# Profile preservation fixtures

Unabridged Bridge profile JSON copied from
DIGI-UW/openelis-analyzer-bridge commit
`1890919f855734ec43fd13945be87a4086e1f17a`, paths
`src/main/resources/analyzer-profiles/fluorocycler-xt-v3.json` and
`src/main/resources/analyzer-profiles/genexpert-astm-v5.json`.

These are test evidence, not OE2-owned runtime defaults. Tests edit a field and
compare the entire submitted profile to the original with that one intentional
change. Drafts exclude Bridge-generated catalog metadata, matching the real draft API. The synthetic `new-file-profile.json` is the exact document entered through the new FILE creation test; its control prefix is test evidence only. Bridge remains responsible for validation, immutable publication, and
runtime execution. These component tests do not prove analyzer traffic.
