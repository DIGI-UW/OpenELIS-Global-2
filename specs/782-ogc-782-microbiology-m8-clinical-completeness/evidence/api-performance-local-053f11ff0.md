# Microbiology API performance qualification

- Commit: `053f11ff0`
- Overall: **PASS**
- Percentiles: `nearest-rank-ceiling`
- Warm-up iterations: 5 per operation
- Measured iterations: 20 per operation

## Environment

- OS: `Mac OS X 26.5.2`
- Architecture: `aarch64`
- Java: `21.0.5`
- Processors: `18`
- Spring: `6.2.17`
- Database: `PostgreSQL 14.4 (Debian 14.4-1.pgdg110+1)`
- Max heap bytes: `17179869184`
- HTTP harness: `Spring MockMvc`

## Data volume

- Worklist cases: 200
- Dense-case isolates: 5
- Dense-case AST readings: 80
- Dense-case timeline events: 91

## Measurements

| Operation | Threshold (ms) | p50 (ms) | p95 (ms) | max (ms) | Result |
| --- | ---: | ---: | ---: | ---: | --- |
| worklist-load | 2000.000 | 4.174 | 5.349 | 5.514 | PASS |
| worklist-search | 500.000 | 3.226 | 3.763 | 3.763 | PASS |
| worklist-filter-page | 300.000 | 3.281 | 3.628 | 4.320 | PASS |
| case-load | 1000.000 | 3.749 | 4.461 | 4.776 | PASS |
| isolate-save | 500.000 | 1.098 | 1.217 | 1.499 | PASS |
| ast-reading-save | 500.000 | 1.949 | 2.305 | 2.875 | PASS |
| timeline-save | 500.000 | 2.689 | 3.808 | 8.329 | PASS |

The companion JSON artifact retains every measured sample. Fixture construction
was outside measured intervals. Against the pre-batching `035d85195` baseline,
worklist-load p95 improved from 117.622 ms to 5.349 ms, search from 115.746 ms
to 3.763 ms, and filtered-page load from 119.081 ms to 3.628 ms. This local
baseline does not replace browser-visible or deployment-environment evidence.
