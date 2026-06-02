# PRFlow — PR Triage Report (short version)

**Repository:** `DIGI-UW/OpenELIS-Global-2`  
**Date:** 2026-04-23  
**Open PRs in snapshot:** 253  
**Base branch:** `develop`  
**Data used:** Authenticated GitHub API (reviews + checks + mergeable state + last human commit)

## Quick summary

- Biggest blocker is still **review approvals**, not merge conflicts.
- Most "ready" PRs are actually just **behind** (need update branch), not broken.
- Dependabot queue is mostly mergeable, but a few need real triage.
- In stale PRs, a couple are approved already and just rotting in conflict.

---

## A) 10 community PRs (non-`pmanko`, non-`dependabot`) ready to move

| PR | Author | mstate | Approvals | Changes req | CI (✓/✗) | Note |
| -- | ------ | ------ | --------: | ----------: | :------- | ---- |
| [#3432](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3432) | Lemeri123 | blocked | 0 | 0 | 7 / 0 | tiny fix, blocked only by review |
| [#2900](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2900) | shreyaakalra | behind | 0 | 0 | 7 / 0 | simple validation cleanup |
| [#2880](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2880) | shreyaakalra | behind | 0 | 1 | 7 / 0 | needs author response to requested changes |
| [#3470](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3470) | SHASHANKKUSHWAHA777 | behind | 0 | 0 | 1 / 0 | small i18n fix |
| [#3419](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3419) | noel-mugisha | behind | 0 | 0 | 1 / 0 | docs typo fix |
| [#2882](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2882) | shreyaakalra | behind | 0 | 0 | 9 / 0 | translation keys only |
| [#2899](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2899) | shreyaakalra | behind | 0 | 0 | 6 / 0 | small bug fix |
| [#3468](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3468) | sagarkhandagre998 | behind | 0 | 0 | 7 / 0 | security header updates |
| [#3386](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3386) | luizgdsmdev | behind | 0 | 0 | 4 / 0 | Playwright test quality improvement |
| [#3361](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3361) | luizgdsmdev | behind | 0 | 0 | 4 / 0 | UI refactor |

**Read this as:** technically mergeable work is waiting mainly on reviewer flow.

---

## B) Dependabot queue (11 PRs, separate)

### Auto-merge candidates (CI green)
- [#3446](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3446), [#3437](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3437), [#3435](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3435)
- [#3477](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3477), [#3423](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3423), [#3424](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3424)
- [#3384](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3384), [#3383](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3383)

### Needs human triage (failing checks)
- [#3410](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3410), [#3402](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3402) — same log4j upgrade pair, same CI failures.
- [#3476](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3476) — 2 failing checks.

---

## C) 10 stale PRs (review for close / refresh)

| PR | Author | mstate | Approvals | CI (✓/✗) | Why it matters |
| -- | ------ | ------ | --------: | :------- | -------------- |
| [#2332](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2332) | samuelmale | dirty | 0 | 2 / 2 | very old + conflicted |
| [#2343](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2343) | krishpatel2-prog | dirty | 0 | 3 / 0 | very old + conflicted |
| [#2493](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2493) | vidishadeswal | dirty | 0 | 7 / 0 | still active but conflict loop |
| [#2518](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2518) | vidishadeswal | behind | 0 | 7 / 0 | labeled stale but actually rebaseable |
| [#2536](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2536) | Surajiitmjnu | dirty | 0 | 4 / 4 | conflicted + failing CI |
| [#2559](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2559) | Abhineshhh | dirty | 0 | 3 / 0 | old + conflicted |
| [#2573](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2573) | OsauravO | dirty | 1 | 7 / 2 | approved once, now stuck |
| [#2612](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2612) | OsauravO | dirty | 1 | 9 / 0 | approved once, now stuck |
| [#2640](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2640) | ISHANK1313 | dirty | 0 | 0 / 0 | stale + no recent CI signal |
| [#2686](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2686) | HerbertYiga | dirty | 0 | 0 / 0 | stale + no recent CI signal |

---

## What this means for PRFlow (simple)

- Add **auto-update branch** for `behind + CI green + no changes requested`.
- Add **auto-merge path** for safe Dependabot PRs.
- Add **state-based stale handling** (not only age):  
  `approved-but-conflicted`, `stale-but-active`, `stale-no-ci`.
- Add **reviewer load balancing** because approval bottleneck is obvious.

---

## Total count

- Total open PRs in repo: **253**
- PRs analyzed in this report: **31**
  - Community ready-to-move PRs: **10**
  - Dependabot PRs: **11**
  - Stale PRs: **10**
- With at least 1 approval (in analyzed set): **2**
- With 0 approvals (in analyzed set): **29**
- Dependabot auto-merge candidates (CI green): **8**
- Dependabot needing triage (failing checks): **3**
