# OGC-788 Evidence Index

## Exact Sources

- Runtime implementation: `b62e28f9ed7f65840ee3b35568a1f8ff389473d9`
- Standardized presentation: `12dfb7fbc62813be2a320e9e473a53b40206690a`
- Comparison base: `c63a6647c188d173702d3c445f2804af62abaa8f`
- Pinned code-qa: `30528d176bd128b4765242d130f38ca9fb85d7b8`

## Records

- [Automated validation](validation-2026-08-04.md)
- [Code-qa review](code-qa-2026-08-04.md)
- [M-08 visual comparison](mock-comparison-2026-08-04.md)
- [UAT contract](../uat.md)

## M2 Broader Administration

- Behavioral implementation and evidence-capture source:
  `93ad7e5921380cbd933fcc398e4469f85b8a9fc8`
- Comparison base: `646ede9b7`
- [Automated validation](validation-m2-2026-08-05.md)
- [Code-qa review](code-qa-m2-2026-08-05.md)
- [M-08 visual comparison](mock-comparison-m2-2026-08-05.md)

The exact external M2 evidence bundle is at
`/Users/pmanko/.codex/visualizations/2026/08/04/019fca12-4b0c-71d0-a37e-8493de64fee5/ogc-788-m2-evidence-93ad7e59/`
and its sibling zip. It contains seven deterministic screenshots, contact
sheets, a manifest, and the 45.64-second H.264/yuv420p MP4.

The external M1 binary bundle is currently at
`/Users/pmanko/.codex/visualizations/2026/08/04/019fca12-4b0c-71d0-a37e-8493de64fee5/ogc-788-m1-evidence/`
and its sibling zip. Live Review-overlay and deployment-guard links are recorded
below; exact deployment identity is served by the guard rather than copied into
this repository.

Automated evidence is pre-UAT proof. It does not replace human Pass/Fail/N/A
rulings in the review overlay.

## Live Review Handoff

- Phrases application: `https://phrases.openelis-global.org/`
- Macro Library: `https://phrases.openelis-global.org/admin/MacroLibrary`
- Review checklist:
  `https://phrases.openelis-global.org/__review/uat-amr.json`
- Deployment guard: `https://phrases.openelis-global.org/__review/target.json`
- Checklist revision:
  `57379544553a890c9644183a09bd8f32fa17c11587cd649bbd659265bbdc62b8`

The shared source contains 22 stories and 67 stable steps. Host filtering gives
the phrases picker exactly the two OGC-788 stories and twelve required steps.
Automated validation and video proof are complete; exact deployment verification
and human review marks remain pending.
