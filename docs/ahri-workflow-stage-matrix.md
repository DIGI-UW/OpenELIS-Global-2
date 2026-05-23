# AHRI Workflow Stage Matrix (SRS traceability)

Machine-readable source: [`volume/configuration/backend/workflow-registry/ahri-workflows.csv`](../OpenELIS-Global-2/volume/configuration/backend/workflow-registry/ahri-workflows.csv)

## Scope

All 12 departments in `research-lab-linkages.csv`. Each row maps:

- **Department** → **workflowType** → **stage** → **allowed SRS personas**

## SRS references

| Section | Content |
|---------|---------|
| SRS §2.3 | User classes (six lab personas) |
| SRS §4 p.59–61 | Access control matrix |
| SRS §3 | Per-lab workflow narratives |
| SRS §4 p.79+ | Per-lab functional requirements |

## workflowType routing keys

| workflowType | Department |
|--------------|------------|
| `traditional_medicine` | Traditional & Modern Medicine Research Lab |
| `bioanalytical` | Bioanalytical Laboratory |
| `bioequivalence` | Bioequivalence Laboratory |
| `immunology` | Immunology Laboratory (Immunology) |
| `pathology` | Pathology Laboratory |
| `bacteriology` | Bacteriology Laboratory |
| `mntd` | Malaria and Neglected Tropical Disease (MNTD) Laboratory |
| `pharmaceutical` | Pharmaceuticals Laboratory |
| `viral_vaccine` | Viral Vaccine |
| `tuberculosis` | Tuberculosis Laboratory |
| `medlab` | CTD (Clinical Trial / Medical Laboratory) |
| `biorepository` | Biorepository Laboratory |

## Gaps / notes

- **Genomics & Bioinformatics** is not in the AHRI research-lab allowlist; `GBDWorkflowTab` remains legacy.
- **Viral Vaccine** UI uses `VirologyLabWorkflowTab` pages; registry stages align to current frontend defaults.
- **Immunology** must use `workflowType: immunology` (not generic `NotebookWorkflowTab` fallback).

## Maintenance

When adding a stage, update:

1. `ahri-workflows.csv`
2. `frontend/src/constants/ahriWorkflowRegistry.js`
3. Default pages in the lab’s `*WorkflowTab.js`
4. Liquibase template seed (if used)
