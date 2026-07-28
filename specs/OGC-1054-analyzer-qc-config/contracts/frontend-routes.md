# Analyzer Frontend Route Contract

## Principles

- The URL is the source of truth for bookmarkable page and filter state.
- Query parameters are serialized in the order shown below.
- Empty/default values are omitted.
- Saved-analyzer setup steps have distinct paths.
- `returnTo` may contain only an absolute application path beginning with one
  `/`. Protocol-relative (`//...`), absolute external, malformed, and
  non-analyzer paths fall back to `/analyzers`.

## Management Routes

| Surface | Canonical route |
| --- | --- |
| Analyzer list | `/analyzers?search=&status=&testUnit=&analyzerType=` |
| Profile catalog | `/analyzers/types?search=&protocol=&readiness=` |

## Setup Routes

| Step | Canonical route |
| --- | --- |
| Instrument | `/analyzers?add=1&step=instrument&profile=<id>&returnTo=<encoded-path>` |
| Verify | `/analyzers/{id}/mappings?setup=1&step=verify&profile=<id>&returnTo=<encoded-path>` |
| Connect | `/analyzers/{id}/edit?setup=1&step=connect&profile=<id>&returnTo=<encoded-path>` |
| Review | `/analyzers/{id}/review?setup=1&step=review&profile=<id>&returnTo=<encoded-path>` |

The selected profile is setup context, not persisted profile-library state.
After analyzer creation it is retained for presentation/navigation and is never
resubmitted during edit.

## QC Detours

| Action | Route |
| --- | --- |
| Configure rule | `/analyzers/{id}/qc-rules?returnTo=<encoded-verify-route>` |
| Add control lot | `/analyzers/qc/control-lots/new?analyzerId={id}&returnTo=<encoded-verify-route>` |

Save and cancel both return through the validated `returnTo`. Missing or invalid
values fall back to `/analyzers`.

## Breadcrumbs

| Surface | Breadcrumb |
| --- | --- |
| Analyzer list | Analyzers |
| Profile catalog | Analyzers → Analyzer types |
| Instrument | Analyzers → Add analyzer |
| Verify | Analyzers → `{analyzer name}` → Verify |
| Connect | Analyzers → `{analyzer name}` → Connect |
| Review | Analyzers → `{analyzer name}` → Review |
| QC rule | Analyzers → `{analyzer name}` → Verify → QC rule |
| Control lot detour | Analyzers → `{analyzer name}` → Verify → Control lot |

The final breadcrumb is current text. Every preceding breadcrumb is a React
Router link to a valid application route.
