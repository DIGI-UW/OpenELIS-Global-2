# Microbiology API performance qualification

- Commit: `035d85195`
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
| worklist-load | 2000.000 | 112.935 | 117.622 | 131.686 | PASS |
| worklist-search | 500.000 | 112.916 | 115.746 | 136.390 | PASS |
| worklist-filter-page | 300.000 | 110.707 | 119.081 | 176.399 | PASS |
| case-load | 1000.000 | 2.632 | 3.077 | 4.197 | PASS |
| isolate-save | 500.000 | 0.952 | 1.123 | 1.217 | PASS |
| ast-reading-save | 500.000 | 1.570 | 1.815 | 1.944 | PASS |
| timeline-save | 500.000 | 2.215 | 2.361 | 2.404 | PASS |

The companion JSON artifact retains every measured sample. Fixture construction
was outside measured intervals. This local baseline does not replace the pending
browser-visible qualification or a deployment-environment run.
