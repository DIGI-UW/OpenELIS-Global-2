# Analyzer remediation rollout

Control recording now requires a matching active control lot for the mapped
test, instrument and reported control identity/level. The former
single-active-lot fallback has been removed: unmatched controls remain held with
a visible reason. Before routine reception, review each instrument's control
identifiers and levels, configure the corresponding active lots, and verify a
control run through the analyzer Quality Control action. Recover held controls
after configuration; do not resend them as new runs merely to clear the queue.
