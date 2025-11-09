## Odoo Sync Queue Overview

OpenELIS provides a dedicated queueing layer for Odoo invoice synchronization
whenever the live connection is unavailable or an invocation fails.

### Backend services

- `OdooIntegrationService#createInvoice` enqueues failed payloads when
  `enableOdooQueue` is set to `"true"` in the configuration properties.
- The queue is stored in the `odoo_sync_queue` table and manipulated through
  `OdooSyncQueueService`.
- Manual REST endpoints:
  - `GET /api/odoo/queue` – returns current queue contents, pending/failed
    counts, and connection status.
  - `POST /api/odoo/queue/retry` – triggers an on-demand processing run through
    `OdooRetryJob` and responds with the updated queue state and status message.
    This call now requeues items that have been stuck in the `PROCESSING` state
    beyond a configurable timeout
    (`org.openelisglobal.odoo.retry.processingTimeoutMinutes`, default `1`
    minutes) and acquires pessimistic row locks to prevent multiple workers from
    handling the same entry concurrently.  
    **New:** retries now validate the stored `partner_id`. If the referenced
    partner has been removed in Odoo, the job will recreate or remap the partner
    using local patient data, update the stored payload, and continue
    processing.
- The scheduled retry job still exists but is disabled by default
  (`org.openelisglobal.odoo.retry.enabled=false`). Re-enable it via
  configuration if automated retries are desired.

### Frontend management UI

- The React admin console includes an **Odoo Sync Queue** page
  (`frontend/src/components/admin/odoo/OdooSyncQueue.js`) accessible from the
  Admin sidebar.
- This view calls the REST endpoints, displays connection status, queue counts,
  each queued item, and offers buttons to run manual retries or refresh the
  data.

### Using the queue

1. Set `enableOdooQueue` to `"true"` so failed invoices are recorded.
2. Ensure Odoo credentials are valid; otherwise queued items will continue to
   accumulate while the connection is offline.
3. Use the admin UI or `POST /api/odoo/queue/retry` to process queued invoices
   once connectivity is restored. Optionally re-enable the scheduled job for
   unattended retries. Items stuck in `PROCESSING` longer than the configured
   timeout are automatically returned to the retry pool the next time the job
   runs.
4. If Odoo records (such as partners) were deleted while the queue accumulated,
   simply run the retry job—partner references are healed automatically and the
   updated payload is persisted for future attempts.
