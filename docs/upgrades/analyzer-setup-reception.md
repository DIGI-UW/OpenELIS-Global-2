# Setup reception when upgrading

After this change, traffic explicitly marked as setup by the Bridge, traffic for
an analyzer in Validation, traffic for an inactive analyzer still in Setup, and
traffic for an Inactive analyzer with no recorded first activation are retained
as setup evidence. They cannot become clinical or QC results later, even after
activation. This prevents setup samples from being accepted as patient results.

Before upgrading an existing site, inventory analyzers in Setup with Active off
and Inactive analyzers with no recorded activation, including any used
operationally despite that state. Do not assume there are none. Complete their
mappings and routine activation through the application before resuming routine
instrument traffic. Review the Bridge runtime state as well as the saved
OpenELIS state. Capture a setup sample, then a separate routine sample after
activation, and verify their respective destinations.

Clinical activation requires the Bridge to acknowledge the `ROUTINE` reception
purpose explicitly. Retirement and deletion of a paired analyzer require its
Bridge runtime to confirm reception is stopped, including after a failed local
save or compensation. Resolve an unavailable Bridge before retrying these
actions.

If a routine sample arrives while its analyzer is still in setup, retain the
receipt for audit and follow the laboratory's repeat/reconciliation process
after correcting the setup. Do not convert retained setup capture into a
clinical result or use a database status edit to bypass verification.
