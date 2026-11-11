# Query layer adoption — TanStack Query v4

Branch: `feat/tanstack-query-layer`, off `develop`, in its own worktree
(`~/code/worktrees/tanstack-query-layer`). The reloads are legacy admin code
that predates the microbiology stack, so this PR does not depend on it.

**Baseline for criterion 6:** develop @ `d6bab7a5a`, E2E run `33887282951`
(2026-09-04): every job green — Cypress Admin / Independent / Core, Playwright
Core 1/2 and 2/2, Playwright Harness 1/2 and 2/2.

## Goal

Give the frontend one way to fetch, cache and refetch server data, and use it to
remove every `window.location.reload()` and same-route `assign()` that today
stands in for "show me this screen's data again".

## Why

The constitution and AGENTS.md declared SWR 2.0.3 as the data layer, but it was
never installed and no file uses it. All 735 `getFromOpenElisServer` calls in
273 files hand-roll fetch inside `useEffect`. With no cache to invalidate, a
document reload was the only refetch primitive anyone had, which is why 84
screens reload after a save and 13 more navigate to their own URL to the same
end. A naive replacement (remount the routed subtree) was tried on #4196 and
reverted: it fixed nothing and broke the analyzer accept-results flows, because
a remount does not refetch a screen whose state lives above it. The fix has to
give each screen a real refetch.

TanStack Query v4 over SWR: hierarchical query keys let one
`invalidateQueries(["case", id])` refresh every panel of a microbiology case
after a write, and `useMutation` replaces the `saving` / `.finally` state every
form re-implements (the missing `.catch` handling fixed on #4196 was a symptom).
v4 is the React 17 line; v5 needs React 18.

## Acceptance criteria

1. `@tanstack/react-query` v4 in `package.json`; one `QueryClientProvider` at
   the app root; `getFromOpenElisServer` wrapped once as the shared query
   function so no other call site changes to opt in.
2. Every `window.location.reload()` outside the session/login/CSRF set is gone,
   replaced by one of: `invalidateQueries` on a converted screen, or `resetForm`
   on a form-only screen. **No screen reloads on error** (today several do,
   destroying user input).
3. Every same-route `window.location.assign()` (Validation, AnalyserResults,
   SearchResultForm, TestOrderability, BatchTestReassignment, report indexes) is
   a refetch, not a navigation. Genuine cross-screen moves use `history.push`.
4. Login, `SecureRoute`, `LandingPage`, `RedirectOldUI`, the CSRF-expiry reload
   in `Utils.ts` and the recovery button in `RouteErrorBoundary` are unchanged.
   The last one reloads after an error boundary has tripped, when component
   state is no longer usable and there is nothing to refetch into.
5. Each converted screen keeps a unit test proving the write triggers the
   refetch (or the form reset) — Red first.
6. **Per-job E2E comparison against the pre-branch baseline**: no job that was
   green goes red. The analyzer accept-results flows and Cypress admin are the
   canaries; both broke last time.
7. Constitution and AGENTS.md describe the layer that is actually installed.

## Status

Draft PR #4213, opened once the pattern was fixed so per-job E2E accumulates
while the conversion continues. The E2E workflow sets `cancel-in-progress: true`
keyed on the PR, so every push kills the run before it. Pushes are therefore
grouped into checkpoints and each run is allowed to finish, otherwise criterion
6 collects nothing.

Once, a GitHub-side branch update landed a develop merge on top of an earlier
push rather than the latest one, and origin briefly did not have this branch's
four newest commits. Nothing was lost locally; reconciled with a plain merge and
a fast-forward push, not a force-push.

| Step                                                        | State                           |
| ----------------------------------------------------------- | ------------------------------- |
| 1 Dependency, provider, shared query function, first screen | done                            |
| 2 Stop reloading on error                                   | done — 20 sites across 17 files |
| 3 List/queue screens refetch after a write                  | in progress                     |
| 4 Same-route `assign()` becomes a refetch                   | done                            |
| 5 Cross-screen `assign()` becomes `history.push`            | in progress                     |
| 6 Per-job E2E comparison                                    | green on `be431ee83e`           |

Counts, non-test source:

|                                                  | Start | Now | In scope |
| ------------------------------------------------ | ----- | --- | -------- |
| `window.location.reload()`                       | 84    | 2   | 0        |
| same-route / cross-screen `assign()` or `href =` | 85    | 40  | 20       |

Criterion 2 is met: no `window.location.reload()` remains outside the session
and error-recovery set. The two that stay are the CSRF-expiry reload in
`Utils.ts` and the recovery button in `RouteErrorBoundary`, both listed in
criterion 4.

Two were removed rather than converted: the reflex-rule and calculated-value
forms each carried a delete handler nothing called, reachable from no button,
whose success path reloaded.

Criterion 3 is done; criterion 5 has started. `TestOrderability` and batch test
reassignment each had Cancel buttons that navigated to the screen they were
already on to forget a pending change; those are resets now. `UomCreate` and
`UserAddModify` left their screen by downloading the app again to reach a route
the router already serves; those are `history.push`.

Criterion 5's count needed two corrections first. `ChangePassword.jsx`'s two
sites are session-boundary, same family as Login and SecureRoute, so they join
the criterion-4 exempt set (19 total now, not 17). `navigate.ts`'s one real site
(three more of its matches were inside a doc comment) is a `single-spa`
micro-frontend interop utility with zero callers anywhere in `src/` — out of
scope, not a screen navigation. In scope for criterion 5: 34 sites across
roughly 20 files.

The four case-view dashboards (Cytology, Pathology, Immunohistochemistry,
program) were first: each sent the browser to its own detail route on a row
click, now `history.push` through a `useHistory()` the router already provides.
`caseViewNavigation.test.jsx` covers all four, Red-proved on Cytology by
reverting to the `href` write.

Next, the admin add/modify screens: `OrganizationAddModify`,
`ExternalConnectionAddModify` and `TestNotificationConfigEdit` each leave for a
different route and component (their own list), so all of their sites —
missing-ID guard, save success, Exit — are `history.push`, `history.replace` for
the guard. `TestNotificationConfigMenu` looked the same shape but wasn't: its
two Exit buttons target the route the component is already mounted at.
`history.push` to your own current route is a no-op in React Router — it would
have left the unsaved checkbox toggles on screen instead of discarding them.
Caught by reading the route table before writing the fix, not by the test. Those
two are a refetch (`discardPendingChanges`, the same shape as
`TestOrderability`'s Cancel), same-route despite being discovered during the
cross-screen sweep. All ten sites Red-proved by reverting to the original write.

The validation queue is the first screen where a same-route `assign()` was a
refetch rather than a reset. Three of them: a per-row action succeeding, a row
found stale under another validator, and the bulk release of the Clear lane.
Each sent the browser to `/validation` plus the search key the page already
held, and the queue came back only because `SearchForm` re-reads
`window.location.search` on mount and re-runs the search from it. Removing the
`assign()` therefore meant giving that re-run a name: `SearchForm` publishes it
to the parent whenever its endpoint changes, and the parent hands the queue a
way to call it. `Validation` reads entirely from props, so nothing had to be
un-mirrored; what it does clear is the row state the released rows carried, the
page number, the open review panels, and the QC acknowledgment scoped to the
batch just released. A refresh serves page 1, which is what the reload did.

`AnalyserResults` and `SearchResultForm` close out criterion 3. Both had the
same shape as validation: a parent that already owns the search endpoint and its
fetch, and a child whose save handler sent the browser to that same endpoint.
Both now call a `refreshResults` the parent passes down instead.
`AnalyserResults`'s parent renders it directly, so the callback is a plain prop;
`SearchResultForm`'s parent renders the search form and the results table as
siblings, so it uses the same registered-ref pattern as validation. Every
criterion-3 target named in the acceptance criteria (Validation,
AnalyserResults, SearchResultForm, TestOrderability, BatchTestReassignment,
report indexes) is now a refetch or a reset, not a navigation.

### One constraint the conversion has to respect

TanStack's structural sharing returns the same object reference when a refetch
produces deep-equal data, so an effect keyed on that data does not run again
after `invalidateQueries`. Seeding local state from a read that way therefore
never re-seeds. What a screen shows has to be derived — the pending edit on top
of the read, `pendingEdits ?? data?.field` — and the edit cleared explicitly on
save and on discard. Reloading the document used to clear it.

The three order screens were converted the mirrored way first and lost the
ability to throw a pending reorder away: discarding is exactly the case where
the stored order has not changed. Their test passed because it changed the
stored order first, which is the one thing discarding never does.

### What E2E can and cannot say here

The config editor is covered twice and the validation queue once. The earlier
claim that one screen was covered undercounted: it missed the Cypress side
entirely, and the Cypress spec is the one that found the bug.

`general-configurations.spec.ts` opens the general-configuration editor, toggles
a value, saves, and asserts the editor has closed and the list is back. That
worked because reloading the document destroyed the parent's record of being in
the editor — the reload was the navigation, not a refetch — so replacing it with
one meant giving the editor a way to say it is finished. Converting it as a
refetch would have left the editor open and turned that job red.

`generalConfigurations.cy.js` goes further: it edits nine configuration menus,
and for the printed-report and validation menus it **types** into the value
rather than picking a radio. Typing is a chain that requires the input to stay
put between clearing it and filling it, which is what caught the route problem
below. The four `MenuConfig/*.cy.js` specs traverse the same list component.
Both sit in the Cypress Independent shard.

`validation.cy.js` reaches the validation queue from the side menu, checks the
heading, and runs a search. It never takes a row action: its `validateTestUnit`
call is commented out. So it guards the load-and-search path the refresh
re-runs, which is worth having, and says nothing about the three refetches
themselves. `validationRefresh.test.jsx` is the guard for those, and it drives
the real screen: the mount search, the per-row retest through the review panel,
and the bulk release through the signature button, each asserting a row only the
server knows about appears afterwards.

No spec opens any of the others. The specs that reach admin go to
`/MasterListsPage` and from there to the test catalog editor, label presets,
calendar management and the site menus. For those screens the per-job comparison
is a regression check on what they all share — the query client at the app root
and the reloads removed from error paths in 17 files — and says nothing about
the screens themselves. Their unit tests are the only guard, which is why each
is written to fail when the behaviour is reverted.

### What the reloads were hiding: routes that rebuild their screen

`Admin` named 9 screens and `App` 137 with `component={() => <Screen />}`. React
Router calls `createElement` on `component`, so an inline arrow is a new
component type every render: React discards the mounted screen and builds a new
one. A notification appearing or expiring is enough. Reloading after each save
hid it; once the reloads went, an open editor could be discarded mid-edit.

`generalConfigurations.cy.js` caught it — it clears the value field then types,
and the field vanished in between ("subject detached from the DOM"). The radio
menus passed because a click is one command.

All are `render` now, which is called as a function and leaves the component
type alone. Every arrow was zero-argument, so no route props were being passed
anyway; `component={BoxDetails}` was already stable and is untouched.
`ConfigMenuDisplay.test.tsx` pins both halves.

### One spec asserted on the reload itself

`esig-result-validation.spec.ts` waited for a `framenavigated` event on
`/validation` after signing a release, so cleanup could not collide with it. No
reload, no event, 30s timeout. It syncs on the reread's GET instead, then
asserts the page is still the queue. Second spec to name the reload rather than
the outcome; expect more.

### Per-job E2E, branch vs baseline

Baseline: run `33887282951` on develop `d6bab7a5a`, every job green. Current:
run `34186122749` on `be431ee83e` — merge, validation refresh, both route fixes,
reworked e-signature spec.

| Job                    | Baseline | Branch                     |
| ---------------------- | -------- | -------------------------- |
| Cypress / Admin        | green    | green                      |
| Cypress / Core         | green    | green                      |
| Cypress / Independent  | green    | green on re-run, see below |
| Playwright Core 1/2    | green    | green                      |
| Playwright Core 2/2    | green    | green                      |
| Playwright Harness 1/2 | green    | green                      |
| Playwright Harness 2/2 | green    | green                      |
| E2E Suite Gate         | green    | green                      |
| Shared Build           | green    | green                      |
| Static                 | green    | green                      |
| Image                  | green    | green                      |

Both canaries (Cypress Admin, Playwright Harness — the analyzer accept-results
flows that broke on the reverted attempt) are green. Cypress Independent
confirms the route fix on the test that found it: `generalConfigurations.cy.js`
goes from 6 passing / 1 failing / 3 skipped to 10 passing.

Two things that cost time and will recur:

- Independent failed once with no test result at all — the runner image no
  longer ships Chrome and the job's apt fallback failed three times, so it
  exited before a single spec ran. Check for a `Running:` line before reading a
  red Cypress job as a code failure; the fix is a job re-run.
- The image build was stuck for several pushes on expired `bullseye-security`
  metadata (`apt-get update` exit 100, expiry growing each run, so no retry
  would help). Develop's #4209 fixed it with `archive.debian.org`. That same PR
  turns the unified results worklist on in changeset 089 and rewrites the two
  legacy result-entry specs, which is why the branch had to merge develop before
  its E2E meant anything.

`E2E / Tests` runs as a `workflow_run`, so it reports develop's branch and sha
and never appears against the PR's own commit: read it through the
`03 Checkpoint - E2E` status instead of `gh pr checks`. Its concurrency is keyed
on the triggering run's sha, so a run already started survives a later push;
only `03 - E2E` itself cancels per PR.

### Automated review findings

Copilot and Codex both reviewed this PR (against `8b253dfd76`, three commits
behind the fixes below). Four findings, each checked against current code before
acting rather than taken on trust:

- `DictionaryManagement.jsx` (Copilot): add/edit/deactivate showed a
  notification but never refetched, so the table kept showing pre-write data.
  Confirmed real; fixed with `refreshDictionaryList()`.
- `ProviderMenu.tsx` (Codex): deactivating a row left `selectedRowIds` pointing
  at it, so Modify stayed enabled and crashed looking the row up after the
  refetch dropped it. Confirmed real; fixed by clearing the selection on
  success.
- `PanelOrder`/`SampleTypeOrder`/`TestSectionOrder` (Codex): a successful save
  cleared `pendingOrder` but not `confirmSelection` or the `*OrderListPost`
  array, so the screen stayed in confirm mode and a second Accept reposted
  already-saved entries. Same fix in all three, matching what the discard path
  already did.
- `queryClient.ts` (Codex, P1): a screen's `if (!data) return <Loading/>` guard
  cannot distinguish "still loading" from a failed read. Query reads now use a
  promise-native GET that rejects non-success HTTP responses; the legacy
  callback helper retains its compatibility behavior for the rest of the app.
  The required-read screens with early loading returns render a shared retry
  state, so an error is visible and actionable instead of an endless spinner.
  Automatic retries and reconnect refetches remain disabled: writes explicitly
  invalidate their data, and an unsaved form must never be overwritten by a
  background reread.

Regression tests cover these paths. Subsequent review also identified draft
state that a full page reload had previously cleared: result-row notes must not
carry into the next patient's results, deleted dictionary rows must lose their
selection, and a completed no-change ordering confirmation must close. Queue
refreshes now replace their pagination state even when the response shrinks to
one page or no results. Configuration saves refresh the running application's
configuration; late logo reads and background test-setting reads preserve the
user's unsaved choices and the baseline used to compute the submitted changes.

## Plan

| Step | Work                                                                                                                                       |
| ---- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| 1    | Add the dependency, provider and shared query function; convert one list screen (UserManagement) end to end with tests, to fix the pattern |
| 2    | Form-only screens (TestAdd and siblings): `resetForm`, stop reloading on error                                                             |
| 3    | List/queue screens: `useQuery` + `invalidateQueries` after each write                                                                      |
| 4    | Same-route navigations: refetch instead of navigate                                                                                        |
| 5    | Cross-screen `assign()`/`href=`: `history.push`                                                                                            |
| 6    | Baseline vs branch E2E comparison per job; fix anything that moved                                                                         |

## Out of scope

Migrating the other ~240 files that fetch but never reload. They convert as they
are touched.
