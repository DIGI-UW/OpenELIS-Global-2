# FHIR read access after upgrading

After this upgrade, the FHIR interface at `/api/OpenELIS-Global/fhir` is
read-only and every read needs an explicit permission. Integration accounts that
read FHIR resources are refused until an administrator grants them the new
reader roles. Plan the grants before the upgrade so integrations stop for as
little time as possible.

## What changes

- Reads and searches require a read permission for each resource type, including
  resources pulled in with `_include` or `_revinclude`. Signing in alone no
  longer grants FHIR access.
- Creates, updates, patches, deletes and transaction bundles are refused for
  every account, including administrators.
- The general `/rest/fhir/{resourceType}` proxy is removed. Order-entry form
  definitions remain available at `/rest/fhir/Questionnaire/{id}`.
- The migration adds three roles, FHIR Results Reader, FHIR Patient Reader and
  FHIR Reference Reader, and grants their permissions to Global Administrator
  only. Reception, Results, Validation and other existing roles receive none.

## Before upgrading

1. List the accounts that call the FHIR interface, such as integration engines,
   data pipelines and reporting tools, and the resource types each one reads.
2. Find integrations that write through FHIR or call
   `/rest/fhir/{resourceType}`. They must move to the OpenELIS workflows, the
   dedicated integration endpoints, or reads through
   `/api/OpenELIS-Global/fhir`.

## After upgrading

1. On the Add/Modify User screen, give each integration account only the reader
   roles for the resource types it reads (see
   [Read permissions](../fhir-access.md#read-permissions)).
2. Sign each integration in again so the new permissions apply.
3. Confirm one read per granted resource type succeeds and that a resource type
   without a grant is refused.

Use a dedicated integration account rather than an administrator account; each
reader role grants every record of its resource types in the installation.
