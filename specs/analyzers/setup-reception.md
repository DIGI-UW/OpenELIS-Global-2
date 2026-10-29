# Setup reception decision (OGC-1573)

Status: implementation candidate, to be validated on the shared analyzer server.
Bridge dependency: https://github.com/DIGI-UW/openelis-analyzer-bridge/pull/115.

## Decision

Setup proceeds through Instrument, Connect, Assays, and Verify. A new instrument
can explicitly start receiving setup samples before its local mapping is
confirmed. Connection probing remains non-mutating; starting reception is a
separate, authenticated administrator action.

OpenELIS uses the existing Validation analyzer status with clinical `active`
false. It commands the Bridge's START_SETUP action, which activates transport with
a SETUP reception purpose. ACTIVATE selects ROUTINE reception instead. Purpose
is included in the runtime fingerprint and activated configuration restored
after restart. The Bridge retains purpose with the receipt-time interpretation
before acknowledging the instrument and emits it on the Device as
`analyzer-delivery-purpose` under normalized contract version 1.2. A delayed
or retried SETUP delivery therefore remains setup evidence after routine
activation. Version 1.2 without one valid purpose is rejected; older routine
contracts remain supported. The exact
connection revision, profile reference, acknowledgment, and actor are retained
in the existing activation history with intent SETUP. Instruments previously
activated for routine use cannot enter this mode.

OpenELIS retains every SETUP delivery, and any delivery while the local
analyzer is in Validation, as read-only setup_capture
evidence, without assigning a local result target or processing QC. The original
bundle and receipt identity remain intact. The mapping editor displays this
evidence using the delivered number/text interpretation and units. Neither
mapping application nor routine activation converts a setup sample into a
clinical result. The operator can remove it from review with the existing
audited confirmation; its bundle remains on the delivery receipt.

Normal activation still requires the exact saved and confirmed local mapping
and a matching Bridge acknowledgment. New deliveries after activation follow
normal review. Retrying a setup delivery returns its original receipt and never
changes its setup classification. Deactivation stops setup reception using the
existing command and retains evidence.

The Bridge still owns all parsing and transport. OpenELIS does not parse raw
instrument messages or reinterpret a prior delivery using current settings.
The mock and its transport are prepared by the harness, not browser tests.

## Validation and rollback

Demonstrate setup reception before mapping confirmation, visible typed mapping
evidence, blocked clinical acceptance and QC processing, unchanged evidence
after mapping/activation, and normal acceptance of a subsequent routine message.
Exercise restart, failed acknowledgment, compensation, and retry identity.

The migration expands an existing audit-intent constraint. Its rollback keeps
historical SETUP records and restores the old write constraint as NOT VALID,
so no audit data is deleted and old code can continue writing ACTIVE/INACTIVE.
Before rolling back application code, stop all Validation-mode connections;
older code does not understand setup_capture holds.
