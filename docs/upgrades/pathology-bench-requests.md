# Pathology bench requests

A pathologist now asks the bench for more work (an additional block, slide or
stain) by raising a request on the case. Each request records its kind, the
block or slide it is about, a priority, who raised it and when, and the
instruction in the pathologist's own words. A request is completed, cancelled
with a reason, or reopened. Sending a case to the pathologist while requests are
still open now asks the sender to confirm, listing each open request, and the
override is recorded on the case.

What changes on upgrade:

- The multi-select of request names and the per-request status dropdown on the
  case view are gone. Requests are raised and closed from the new table.
- Requests already on file are kept and listed in the table. They have no kind,
  target, priority, author or date, so those cells read "Not recorded". They can
  be completed, cancelled and reopened like any other. Nothing is converted or
  deleted.
- Saving a case no longer rewrites its requests. Before, every save deleted the
  requests and wrote them again, which gave them new ids. Now a request keeps
  its id and its history for as long as the case exists.
- A case save that still posts `requests`, from a script or an older client, has
  them ignored. Requests are raised only through the new requests endpoint,
  `POST /rest/pathology/caseView/{id}/requests`.
- At a bench stage, a case that holds requests now shows the Pathologist Review
  section. It is read-only apart from the requests, which the bench can work
  from there.
- The upgrade sets `last_updated` on request rows that had none, so every
  request can be changed in place.
- The list of 22 request names is no longer offered. It stays in the database,
  unused by this screen.
- Raising, completing, cancelling and reopening a request each add a line to the
  case history.

No configuration is needed. The schema change is additive (new nullable columns
on `pathology_request`) and the changeset has a rollback.
