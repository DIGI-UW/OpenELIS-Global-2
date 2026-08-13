# Analyzer QC/Configuration Foundation Route Record

**Scope:** Routes implemented by PR #3792. Future Analyzer Type/mapping
milestones may replace them through tested redirects and migration.

Route structure is an OpenELIS engineering decision. It is not sourced from
`openelis-work`; product mocks are used only to validate the visible workflow.

## Principles

- The URL is the source of truth for bookmarkable page/filter state.
- Empty/default values are omitted and supported parameters serialize stably.
- `returnTo` accepts only a same-origin application path beginning with one
  `/`; invalid values fall back to `/analyzers`.
- Every page has one semantic `h1`; preceding breadcrumbs are router links.

## Foundation Routes

| Surface | Route |
| --- | --- |
| Analyzer list | `/analyzers?search=&status=&testUnit=&analyzerType=` |
| Shipped-profile catalog | `/analyzers/types?search=&protocol=&readiness=` |
| Instrument | `/analyzers?add=1&step=instrument&profile=<id>&returnTo=<encoded-path>` |
| Verify | `/analyzers/{id}/mappings?setup=1&step=verify&profile=<id>&returnTo=<encoded-path>` |
| Connect | `/analyzers/{id}/edit?setup=1&step=connect&profile=<id>&returnTo=<encoded-path>` |
| Review | `/analyzers/{id}/review?setup=1&step=review&profile=<id>&returnTo=<encoded-path>` |
| QC rule detour | `/analyzers/{id}/qc-rules?returnTo=<encoded-verify-route>` |
| Control lot detour | `/analyzers/qc/control-lots/new?analyzerId={id}&returnTo=<encoded-verify-route>` |

The `profile` parameter in this branch is setup/bootstrap context, not proof of
a durable reusable profile association.

## Breadcrumb Record

| Surface | Breadcrumb |
| --- | --- |
| Analyzer list | Analyzers |
| Shipped-profile catalog | Analyzers -> Analyzer types |
| Instrument | Analyzers -> Add analyzer |
| Verify | Analyzers -> `{analyzer name}` -> Verify |
| Connect | Analyzers -> `{analyzer name}` -> Connect |
| Review | Analyzers -> `{analyzer name}` -> Review |
| QC rule | Analyzers -> `{analyzer name}` -> Verify -> QC rule |
| Control lot | Analyzers -> `{analyzer name}` -> Verify -> Control lot |

M1-M4 must define their route contract from current OpenELIS router patterns,
accessibility, bookmarkability, and migration needs. They must not copy route or
component structures from a non-technical product artifact.
