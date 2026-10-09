Saving eligible microbiology work opens a case before physical sample
collection. Recording the sample later attaches to that same case through
requested-specimen identity; retries retain one sample, case and owner.
Case-role tests open cases without creating a result analysis. Environmental
work groups by the shared sampling site as well as order, sample type and lab
unit; collected-in-sets cultures remain one case across sites.
Requested-specimen site identity survives later sample recording. New cases
inherit the order's Program only when Show on Microbiology case is enabled in
the shared Programs admin; reused cases keep their Program and routing never
changes the order's Program.

Roadmap step 5 is in progress above #4650 in stack #4651. This PR carries
catalog controls, the registered routing/shared-sample/Program migrations,
shared order-save and analysis-creation routing, an unsaved-order preview, and
shared bottle inputs and environmental per-sample site selection. The site
references reuse the shared sampling-site records maintained by Locations &
Organizations, with nullable registered foreign keys and no fabricated
historical sites. Nonblocking preview warnings cover bottle count, duplicate
containers, different sites, collection interval and explicitly configured
adult/paediatric classification. Microbiology reflex work is filed in its
catalog lab unit; ordinary reflex filing retains the existing behavior.
Case-role reflexes add requested work without a result analysis or an
automatically created sample. Explicit and uniquely configured target types
retain ownership through later collection and retries. Reordering a cancelled
case test creates a new analysis and ownership; the original results and
cancellation history stay intact, including when its catalog switch has since
changed. Shared order editing accepts orders with no receipt date. Program
visibility is opt-in and omitted fields preserve the stored setting during
partial saves and lifecycle changes.

Validation:

- Earlier routing/preview/shared-save checks: 50 focused backend tests and 87
  frontend tests passed; source build and formatting passed.
- Reflex transaction checks: 11 routing tests passed, including real
  before-commit dispatch and rollback of all writes when routing fails.
- Shared fixture isolation: 59 focused tests passed after removing dependence on
  the global active-lab-unit catalog.
- Program slice: 27 backend tests passed, covering eligible/hidden Program
  defaults, reused-case independence, partial-save preservation, shared
  questionnaire storage, and registered application changelog upgrade/rollback.
  The no-database Program mapping test passed in 2.1 seconds; 44 frontend tests
  and the production build passed.
- Environmental slice: 60 focused backend checks passed in one final run
  (routing, preview, set warnings, sample recording, registered migration
  upgrade/rollback and no-database mapping); 40 frontend tests, the production
  build and formatting passed.
- Electronic/reflex/reorder slice: 68 focused backend tests passed in one final
  run, covering electronic acceptance and retries, rollback of accepted status
  plus order/case writes, Case-role reflexes on collected and uncollected
  targets, later collection, active-unit gates, shared order editing, retained
  results/ownership, and reordering after catalog changes. Source build and
  scoped formatting passed.
- Playwright verified login and environmental order entry after the
  electronic/reflex rebuild, with no browser console errors.
- Recorded environmental Playwright checks passed: same-site specimens preview
  one case, a different site previews a second case, and changing back preserves
  selected tests. The final run captured no console errors. Catalog fixtures and
  sites were created through shared admin screens. Compared against the pinned
  M18 order mock: default site plus per-sample site, no separate Microbiology
  section, matching swabs share a case. The complete preview table layout
  remains outstanding.
- The owned development stack rebuilt and booted against its existing database.
  Recorded Playwright checks passed for catalog settings and order preview, and
  for Program enable/save/reload/disable/reload. Program browser checks captured
  no console errors. The earlier catalog run captured existing unmounted-state
  warnings in BasicInfoSection. Screenshots and video remain untracked evidence.

Limitations: the development app logs a local FHIR hostname/certificate mismatch
when mirroring questionnaires; local questionnaire storage succeeds, but this
run does not prove FHIR mirror delivery. Full local CI and the three required
GitHub checkpoints have not been established on the latest revision. The one CI
snapshot taken before the environmental push (on 95e375a5c5) exposed only a
passing Run check, with all three required checkpoints absent. The new routing
revisions have not been polled.

Remaining in this same milestone: existing-case additions,
cancellation/add-before-remove confirmation and result reason, complete preview
referral/tested-elsewhere treatment and mock layout, save-time warning
integration, and complete fresh/upgraded application and browser acceptance.
Reporting-track administration and case Program selection remain with their
owning case-information milestone. No step-5 roadmap checkbox is claimed
complete. Full-stack acceptance and clinical migration remain later gates. No
merge is requested.
