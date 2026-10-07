# Normalized delivery receipts

This OpenELIS follow-up consumes the versioned normalized contract from the pinned
Analyzer Bridge. Bridge remains responsible for file handling and stable
per-accession delivery identity; OpenELIS owns acceptance and clinical processing.

## Acceptance

`POST /analyzer/fhir` validates the normalized envelope and resolves its saved
Bridge connection. Within one database transaction it locks the analyzer row,
looks up `(connection_id, message_id)`, and either returns the original acceptance
summary or performs mapping, staging, operational QC, and receipt insertion.
A unique database constraint provides a second guard against duplicate receipts.
Failures roll back staging and the receipt together. Persistence failures in QC
are propagated rather than acknowledged as successful imports.

Receipts are independent of the staging queue. Removing reviewed staging rows,
changing local mappings, or acknowledging another profile revision does not make
an already accepted delivery new work. The replay response reports the original
acceptance counts, not the current size of the worklist. This applies equally to
patient and control traffic. An intentionally excluded result also receives a
receipt so a later mapping change cannot silently resurrect it on retry.

The receipt includes analyzer/profile identity, original acceptance counts, actor,
and acceptance time. It is not a copied profile or analyzer runtime configuration.
Receipt retention must cover the period in which Bridge deliveries can be replayed;
do not purge receipts as part of result review or file cleanup.

## Upgrade boundary and verification

Receipts begin with deliveries accepted by this implementation. This migration
does not reconstruct message identities for previously reviewed/deleted staging
rows; it must not be described as retroactive exactly-once delivery.

Focused tests cover concurrent receipt creation, distinct message IDs, replay
after staging removal and profile changes, actual QC replay/rollback, held-result
retention, and ORM mappings. Full deployment/restart and assembled instrument
traffic evidence remain separate from these focused test claims.
