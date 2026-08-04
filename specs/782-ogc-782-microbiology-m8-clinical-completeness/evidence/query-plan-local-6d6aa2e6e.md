# Microbiology worklist query-plan qualification

- Commit: `6d6aa2e6e`
- Database: PostgreSQL `14.4 (Debian 14.4-1.pgdg110+1)`
- Command shape: `EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) <DAO query>`
- Dataset: 414 cases, 409 open cases, 18 isolates, 57 AST runs, and 0 critical
  communications
- Decision: no index migration

## Exact DAO Shapes

| Query | Parameters | Rows | Planning (ms) | Execution (ms) | Shared reads | Temp I/O |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Open cases | 0 | 409 | 0.283 | 0.201 | 0 | 0 |
| Sibling cases by sample | 209 | 409 | 0.452 | 0.181 | 0 | 0 |
| Isolates by case | 409 | 13 | 0.208 | 0.041 | 0 | 0 |
| AST runs by isolate | 13 | 52 | 0.249 | 0.051 | 0 | 0 |
| Communications by case | 409 | 0 | 0.275 | 0.023 | 0 | 0 |

The parameterized statements used the full current open-case, sample-item, and
isolate identifier sets, matching Hibernate's `IN (:ids)` query shape. The JSON
evidence records the templates without retaining generated identifiers.

## Planner Review

All plans used in-memory quicksorts and sequential scans. This is an expected
planner choice for these cardinalities because each batch selects nearly every
row in the relevant small table. No plan used disk reads or temporary files,
and all completed at or below 0.201 ms.

The existing indexes cover `micro_case.sample_item_id`,
`micro_isolate.case_id`, `micro_ast_run.isolate_id`, and
`micro_critical_communication.case_id`. The empty communication table means
its plan cannot demonstrate populated-table selectivity, but it also provides
no evidence for another index. API worklist p95 is 5.349 ms and browser
worklist-render p95 is 218.2 ms, both within their qualification budgets.

Adding a migration here would be speculative, so no schema change was made.
