# Access to the OpenELIS FHIR interface

The OpenELIS interface at `/api/OpenELIS-Global/fhir` supports reads and
searches only. It does not accept creates, updates, patches, deletes, or
transaction bundles, including from administrators. Make clinical changes
through the OpenELIS workflows and the dedicated, authenticated integration
endpoints.

## Read permissions

Authentication alone does not grant FHIR access. An administrator grants the
following roles through the existing Add/Modify User screen. Each role supplies
one read permission; combine them only when the integration needs both:

| Role                  | Permission            | Resources available                                     |
| --------------------- | --------------------- | ------------------------------------------------------- |
| FHIR Results Reader   | `fhir.read.results`   | Observation, DiagnosticReport, Specimen, ServiceRequest |
| FHIR Patient Reader   | `fhir.read.patients`  | Patient                                                 |
| FHIR Reference Reader | `fhir.read.reference` | Practitioner, Organization, Location, Device            |

These permissions grant access to **all records of those resource types** in
this installation. They do not restrict access by patient or laboratory unit.
Use a dedicated integration account and grant only the resource types its
integration needs. Sign in again after changing role permissions.

The installation migration grants all three permissions to Global Administrator.
The new reader roles are initially unassigned. It does not automatically grant
them to Reception, Results, Validation, or other existing roles. Existing
integration accounts therefore need explicit grants before their FHIR reads
resume. Requests without the necessary permission are rejected. Included and
reverse-included resources require their own read grants; a search cannot use
`_include` or `_revinclude` to bypass them. Readers with at least one grant can
retrieve `/metadata` to inspect the read-only capabilities.

## Order-entry form definitions

`/rest/fhir/Questionnaire/{id}` serves a form definition to signed-in users for
Generic Sample Order. Its identifier must be a UUID. The existing questionnaire
storage service retrieves the definition from its configured internal FHIR store
or local database. This route cannot choose a different resource type or
destination and cannot retrieve patient responses. The former general
`/rest/fhir/{resourceType}` proxy is removed. Clinical integrations must use the
permission-controlled interface above instead.

## Internal FHIR storage

The separately deployed FHIR store is an application dependency, not this
interface. Its network exposure, service credentials, and certificate trust must
be configured independently. The permissions above do not protect a store that
an operator exposes directly. OpenELIS's internal persistence and analyzer
import services continue to use their existing service paths.
