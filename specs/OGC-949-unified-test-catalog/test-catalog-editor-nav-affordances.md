# Test Catalog Editor — SideNav affordances (decision + UX research)

**Status:** accepted · **Area:** `frontend/src/components/admin/AdminSideNav.jsx`
· **Relates to:** #3504 (single URL-routed editor SideNav), #3716 (v1 sections
M5–M12, merged)

## Context — the problem this fixes

Under #3504 the nine editor sections (Basic Info, Sample & Results, Methods,
Ranges, Sample Storage, Panels, Terminology, Analyzers, Display Order) render in
the SideNav only after drilling into a specific test
(`/TestCatalogEditor/:testId/:section`). Before a test is selected,
**Test Catalogue Management** shows a single child — _Test Catalog Editor_ — so
the editor's breadth is invisible: an admin has no signal that nine configuration
sections exist behind a test selection, which has caused confusion about whether
the feature shipped completely.

The fix keeps the **single left-nav** (no tabs — a product constraint and the
#3504 spec mandate) and makes the hierarchy transparent.

## What the UX research says

- **Keep it in the left-nav.** Carbon's guidance: use the left panel when there
  are _"more than five secondary navigation items, or if you expect a user to
  switch between secondary items frequently."_ Nine sections with frequent
  switching is the textbook case _for_ a side-nav. We have only two tiers
  (category → sections), so Carbon's "use tabs for a third tier" rule never
  applies. Tabs are not needed and are explicitly out.
- **Don't put the test list _in_ the nav.** Carbon: _"Don't place unbounded
  content in the shell side navigation … do not place content that has no upper
  limit (such as created by users) … Instead make use of drill-down patterns."_
  Tests are unbounded, user-created content, so a test selector belongs in the
  list view (`TestCatalogList`, the existing drill-down) — **not** as nav items.
- **Disable, don't hide, the sections.** The cross-source rule for a feature
  that exists but needs a prior selection: _"when an end user has permission but
  hasn't set some other selection that makes it active, use a 'greyed out'
  approach. This informs the user that the feature is available, but THEY need to
  do something to activate it."_ Disabling (vs. hiding) also lets the UI teach
  its own breadth.
- **A disabled control must explain itself.** _"Explain why a feature is disabled
  and also how to re-enable it."_ Use `aria-disabled` (not the native `disabled`
  attribute) so assistive tech announces the item exists but is inactive, and
  pair it with a visible caption.

## Decision

Keep one left-nav. Under **Test Catalogue Management**, always list all nine
sections so the editor's breadth is visible up front, and make them
**state-dependent**:

| State | List item label | Sections | Context line |
| --- | --- | --- | --- |
| No test open | _Test Catalog Editor_ (→ list) | shown, **disabled** (`aria-disabled`, not navigable) | "Select a test to edit its sections" |
| Editing a test | _← All Tests_ (→ list) | live routed links, active one marked | "Editing: _{test name}_" |

- The single list item doubles as the **back-to-list** affordance — its label is
  context-sensitive ("Test Catalog Editor" when browsing, "← All Tests" when
  editing) so there is no redundant second link.
- The **"Editing: {name}"** context line provides wayfinding (which test am I in)
  without repeating the page heading in a heavier component. The name is fetched
  in a self-contained, editor-route-gated effect and **degrades cleanly** to a
  generic "Editing test" label if it can't load — the nav never blocks on it.
- Disabled section items carry `aria-describedby` pointing at the helper caption,
  so screen-reader users get the "select a test" explanation on each item.

This permanently removes the "is a commit missing?" confusion: expanding the menu
now always reveals the nine sections.

## Implemented in this PR

`AdminSideNav.jsx` only (plus its test and four `en.json` keys —
`…testCatalog.backToList`, `.sectionsHelper`, `.editing`, `.editingGeneric`).
`AdminSideNav.test.jsx` asserts the full contract: sections present-but-disabled
with no `href` off an editor route; live routed links with the active one marked
when editing; the contextual back label; and the "Editing: {name}" line with its
generic fallback.

## Deliberately deferred (not in scope here)

- **Typeahead test switcher.** If fast test-to-test switching without bouncing
  through the list is wanted later, the Carbon-sanctioned form is a `ComboBox`
  (a _filtered_ drill-down), never a flat list of tests in the rail.
- **Renaming the list item / group** for clarity ("Test Catalog Editor" as the
  label for what is really the test list) — left as-is to avoid churn on keys
  used elsewhere.

## Sources

- Carbon Design System — UI shell side navigation usage (nesting limits, the
  ">5 items / frequent switching" rule, the unbounded-content drill-down rule):
  <https://v9.carbondesignsystem.com/experimental/ui-shell/usage/>
- Carbon Design System — UI shell left panel:
  <https://carbondesignsystem.com/components/UI-shell-left-panel/usage/>
- The Usability People — Disable, Hide, or Grey Out?:
  <https://www.theusabilitypeople.com/thought_leadership/disable-hide-or-grey-out>
- Smashing Magazine — Hidden vs. Disabled in UX:
  <https://www.smashingmagazine.com/2024/05/hidden-vs-disabled-ux/>
- UX Tigers — Inactive GUI Controls: Show, Disable, or Hide?:
  <https://www.uxtigers.com/post/inactive-buttons>
- Level Access — Accessible Navigation Menus:
  <https://www.levelaccess.com/blog/accessible-navigation-menus-pitfalls-and-best-practices/>
