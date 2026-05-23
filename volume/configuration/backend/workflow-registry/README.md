# AHRI Workflow Registry

Canonical **department × workflow stage × SRS persona** mapping for notebook workflows.

## File

`ahri-workflows.csv` — loaded at startup by `WorkflowRegistryConfigurationHandler`.

## Columns

| Column | Description |
|--------|-------------|
| `departmentName` | Test section name (must match `research-lab-linkages.csv`) |
| `workflowType` | Frontend/backend routing key (e.g. `biorepository`, `immunology`) |
| `stageOrder` | 1-based page order |
| `stageId` | Stable stage identifier |
| `stageTitle` | Display title (matches workflow tab defaults) |
| `allowedPersonas` | Pipe-separated SRS lab personas |

## SRS personas (lab unit only)

Sample Collector, Laboratory Technician, Junior Researcher, Senior Researcher, Lab Manager, Biomedical Staff

## Related

- [docs/ahri-workflow-stage-matrix.md](../../../docs/ahri-workflow-stage-matrix.md) — traceability to SRS §3/§4
- [AHRWorkflowRegistryCatalog.java](../../../src/main/java/org/openelisglobal/common/constants/rbac/AHRWorkflowRegistryCatalog.java)
