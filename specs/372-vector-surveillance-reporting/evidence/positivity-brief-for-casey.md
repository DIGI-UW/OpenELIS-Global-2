# Vector "positivity" — what the field does vs. what OE2 can do today

**Audience:** Casey (V-04 / OGC-585 clarification)
**Author:** Piotr — prepared 2026-06-17
**TL;DR:** Computing real pathogen *positivity* and infection rates requires
*per-test* positivity definitions that OpenELIS does not yet have for vector
analytes. The V-04 dashboard (PR #3732) therefore infers positivity from
**workflow state**, exactly as OE2's existing deconvolution worklist already
does. We need a product decision: build per-test positivity semantics, or accept
and document the workflow proxy for the demo.

---

## 1. How established programs determine "positive"

Verified across CDC, VectorSurv/CalSurv, and peer-reviewed entomology sources:

- **Positivity is per-analyte and threshold-based.** Each pathogen has its own
  positive/negative cutoff (e.g., per-virus RT-qPCR Ct threshold; ELISA OD/score).
  A weak positive **reflexes to a confirmatory assay**. One physical pool, screened
  against multiplex panels, produces **multiple independent per-pathogen results**.
  *(California Mosquito-Borne Virus Surveillance & Response Plan)*
- **"Positive" has a lifecycle: presumptive → confirmed.** Unconfirmed positives
  meaningfully distort surveillance. For malaria, **88% of *P. falciparum*
  CSP-ELISA positives could not be PCR-confirmed**, so the field **confirms every
  positive** before using it; unconfirmed positives inflate the Entomological
  Inoculation Rate. *(Durnez et al. 2011, Malaria Journal 10:195)*
- **The general-LIMS problem is solved by attaching positivity logic to each
  test** — a per-analyte threshold + reflex/confirmation rule layered on coded
  results. There is no single global "what is positive" rule.

## 2. Infection-rate math (for the MIR panel)

- **MIR** = (positive pools / total specimens tested) × 1,000. Assumes **one
  infected mosquito per positive pool**, so it is a **lower bound** that
  **underestimates at high transmission / large pools** (in 2002 Chicago WNV data
  MIR was off by ~half at peak). *(CDC; VectorSurv; Gu et al., J Med Entomol 40(5):595)*
- **MLE / Bias-Corrected MLE** relax that assumption and are the **recommended
  default** in CDC PooledInfRate and VectorSurv; MIR is kept as the fallback when
  all pools are positive. *(VectorSurv infection-rate calculator docs)*

## 3. What OE2 does today

- **No per-analyte positivity definition exists.** Both code paths use proxies:
  - Existing **deconvolution worklist** treats *any non-blank result* on a pool's
    analysis as the "positive test" (`VectorDeconvolutionRestController.java:261`).
    The dictionary lookup there is for **display only**, not interpretation.
  - The **V-04 dashboard** treats a pool as positive when
    `deconvolutionStatus <> 'NOT_APPLICABLE'`
    (`VectorSurveillanceDAOImpl.java`) — i.e., deconvolution was initiated, which
    only happens on a positive pool.
- **No presumptive/confirmed distinction** on results.
- **Hierarchical pool resolution ("deconvolution") *is* implemented** — a real
  match to field practice (`NOT_APPLICABLE→PENDING→IN_PROGRESS→COMPLETE` + sub-pool
  fan-out).
- **MIR** uses the correct classical formula but is **species-level and MIR-only**
  (no per-pathogen breakdown, no MLE).
- **Sporozoite rate is withheld until deconvolution resolution ≥ 95%** — aligned
  with the field's caution about unconfirmed sporozoite positives.

## 4. The decision we need from you

Positivity is not a dashboard bug — it is a **module-wide gap**: OE2 has no
positivity semantics for vector analytes, and everything downstream inherits that.

**Option A — Accept & document the workflow proxy (demo path).**
Positivity = "deconvolution initiated / result entered." Fast, demo-ready,
consistent with the existing deconvolution worklist. Document the definition on
the dashboard and PR. Defer real semantics to a follow-up ticket.

**Option B — Build per-test positivity semantics (production path).**
Add per-vector-test result vocabularies + positive/negative thresholds + a
presumptive/confirmed lifecycle, then compute positivity/MIR from confirmed
results. This is the correct, field-aligned design but is a multi-ticket effort
spanning the whole vector module, not the dashboard alone.

**Recommendation:** A for the demo (clearly labeled), with B captured as a
follow-up epic. The dashboard is structurally ready to swap its positivity source
once B exists.

### Specific questions for you
1. For the demo, is the **deconvolution-initiated** definition of "positive"
   acceptable, or do you need it read from an actual qualitative result value?
2. Is **species-level MIR** sufficient, or is **per-pathogen** MIR required?
3. Should we keep **sporozoite rate withheld < 95% resolution** (recommended), or
   surface it differently?
4. Is there an existing convention (from Indonesia/Silnas or the lab) for which
   vector test result value means "positive" that we should encode first?

---

*Sources: California Mosquito-Borne Virus Surveillance & Response Plan; Durnez et
al. 2011 (Malaria Journal 10:195); Gu et al., J Med Entomol 40(5):595; Bustamante
& Lord, AJTMH (PMC2877431); VectorSurv infection-rate calculator docs; CDC
mosquito surveillance software toolkit.*
