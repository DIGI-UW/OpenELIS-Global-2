The existing microbiology specifications compete with the agreed V2 functional baseline. Consolidate engineering guidance into `specs/amr/spec.md`, `plan.md` and `tasks.md`, with authority boundaries, runtime retirement/rewrite/reuse mapping, clinical/audit preservation, clean cutover rules and a single V00–V16 sequence. Remove the four superseded specification trees and obsolete cleanup roadmap.

Assign one primary owner to all 112 existing V2 acceptance criteria and explicit dependent verification. Transfers preserve separate cases, joining is deferred, and export period membership remains collection-date based independently of deduplication chronology. Planned tests are deliverables, not passing evidence. V00 records verified documentation/tracking completion; V01–V16 remain planned.

Paired [design review #354](https://github.com/DIGI-UW/openelis-work/pull/354); pinned [functional baseline](https://github.com/DIGI-UW/openelis-work/tree/516c88efbdd2edbc9ea108f69d7b57f4ceb9ec9b). The design follow-up pins its engineering backlink; final design revision is 61d7971ff5ae2814cb0e5f3ebd84371f8a0a3659.

Validation: Prettier 3.4.2 check on all three retained documents and `git diff --check` passed. Source links/anchors and 112-criterion coverage were checked; paired gallery tests (276) and build passed, with Chrome rendered review. The final engineering link recheck covered 185 references with zero errors.

Jira readback verified 31 superseded closures, 29 retained shared/future alignments, 17 roadmap tasks and 35 new native dependency links. Retained owners/statuses, independent catalog work and the 1383-blocks-1382 direction were preserved. Confluence walkthrough 1315209256 and graph/diagram copies are archived, with six walkthrough versions retained; its other children were preserved as siblings.

Documentation only: no application APIs, schemas or clinical records change. This draft is unmerged. No application tests, deployment or clinical acceptance are claimed.
