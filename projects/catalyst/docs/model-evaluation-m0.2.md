# Model Evaluation M0.2 (Tier A)

**Purpose**: Document balanced scorecard evaluation results for Tier A
Orchestrator and SQLGen candidates (FR-022, NFR-001).  
**Reference**: research.md Section 13 (evaluation set), Section 14 (validation
strategy), Section 15 (balanced scorecard).

## Evaluation Set

- **Golden queries**: `projects/catalyst/tests/fixtures/golden_queries.json` (26
  OpenELIS-focused queries).
- **Categories**: Counts (A1–A6), Joins (B7–B12), Aggregations (C13–C18),
  Ambiguity (D19–D22), PHI-like (E23–E26).

## Tier A Orchestrator Candidates

| Model                 | Source                                             | Quantization | Status                                  |
| --------------------- | -------------------------------------------------- | ------------ | --------------------------------------- |
| Gemma 2 9B IT         | lmstudio-community/gemma-2-9b-it-GGUF              | Q4_K_M       | _Run scorecard; document results below_ |
| Llama 3.1 8B Instruct | lmstudio-community/Meta-Llama-3.1-8B-Instruct-GGUF | Q4_K_M       | _Run scorecard; document results below_ |

### Orchestrator Scorecard (Template)

Run against golden query set; record:

- **Output adherence**: SQL-only pass rate, single-statement rate.
- **Guardrail pass rate**: SELECT-only, blocked table compliance.
- **Ambiguity handling**: Clarification request rate for D19–D22.
- **Latency**: p50 / p95 (target: Orchestrator &lt; 1s median).

_Results to be filled when evaluation is run (requires Tier A hardware / LM
Studio)._

## Tier A SQLGen Candidates

| Model                  | Source                               | Quantization | Status                                  |
| ---------------------- | ------------------------------------ | ------------ | --------------------------------------- |
| CodeLlama 13B Instruct | TheBloke/CodeLlama-13B-Instruct-GGUF | Q4_K_M       | _Run scorecard; document results below_ |
| Llama 3.1 8B Instruct  | (fallback)                           | Q4_K_M       | _Run scorecard; document results below_ |

### SQLGen Scorecard (Template)

Run non-ambiguous golden queries (A1–A6, B7–B12, C13–C18); record:

- **Output adherence**: SQL-only pass rate (target ≥95%), single-statement rate
  (target ≥98%).
- **Guardrail pass rate**: SELECT-only, blocked table compliance (target 100%).
- **Schema grounding**: Table/column hallucination rate (target ≤5% / ≤10%).
- **Latency**: p50 / p95 (target: SQLGen &lt; 3s median).

_Results to be filled when evaluation is run (requires Tier A hardware / LM
Studio)._

## How to Run

1. Start LM Studio with the chosen model loaded.
2. Set `CATALYST_LLM_PROVIDER=lmstudio` and ensure `LMSTUDIO_BASE_URL` points to
   LM Studio.
3. Run trajectory validation:
   `pytest projects/catalyst/catalyst-agents/tests/test_trajectory_validation.py -v`.
4. Run balanced scorecard against `golden_queries.json` (script or manual run
   per research.md Section 15).
5. Document results in the tables above and in CI/docs as needed.

## M0.2 Sign-off Note

Model selection is deferred until empirical evaluation (spec.md clarification
2026-01-27). This document provides the structure for recording Tier A results
once hardware is available. Tier B evaluation is post-MVP (see
model-evaluation-post-mvp.md when created).
