# Setup reception when upgrading

After this change, traffic explicitly marked as setup by the Bridge, traffic for
an analyzer in Validation, and traffic for an inactive analyzer still in Setup
are retained as setup evidence. They cannot become clinical or QC results later,
even after activation. This prevents setup samples from being accepted as
patient results.

Before upgrading an existing site, inventory analyzers in Setup with Active off,
including any used operationally despite that state. Do not assume there are
none. Complete their mappings and routine activation through the application
before resuming routine instrument traffic. Review the Bridge runtime state as
well as the saved OpenELIS state. Capture a setup sample, then a separate
routine sample after activation, and verify their respective destinations.

If a routine sample arrives while its analyzer is still in setup, retain the
receipt for audit and follow the laboratory's repeat/reconciliation process
after correcting the setup. Do not convert retained setup capture into a
clinical result or use a database status edit to bypass verification.
