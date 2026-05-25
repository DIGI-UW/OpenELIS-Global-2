# AHRI Workflow RBAC — Manual UAT Checklist

Reference: SRS §5 Use Case 2 (Role-Based Access Control)

## Prerequisites

- User accounts with single SRS persona each (Sample Collector, Laboratory Technician, etc.)
- Users assigned to one AHRI research lab (e.g. Biorepository Laboratory)
- Active department selected at login (`loginLabUnit`)

## Per-lab checks

| # | Test | Pass |
|---|------|------|
| 1 | User without active department cannot create notebook entry | ☐ |
| 2 | User in Lab A cannot open Lab B notebook workflow | ☐ |
| 3 | Sample Collector sees only intake stage (stage 1) | ☐ |
| 4 | Laboratory Technician can access processing stages, not disposal-only stages | ☐ |
| 5 | Lab Manager can access all stages for assigned lab | ☐ |
| 6 | Global Admin can access all stages | ☐ |
| 7 | Workflow tab routes via `workflowType` (not wrong lab UI) | ☐ |

## Labs to verify

- [ ] Biorepository Laboratory
- [ ] Immunology Laboratory
- [ ] Bacteriology Laboratory
- [ ] MNTD Laboratory
- [ ] Tuberculosis Laboratory
- [ ] Pharmaceuticals Laboratory
- [ ] Traditional & Modern Medicine Research Lab
- [ ] Bioanalytical Laboratory
- [ ] Bioequivalence Laboratory
- [ ] Pathology Laboratory
- [ ] Viral Vaccine
- [ ] CTD (Medical Laboratory)

## API spot-check

`POST /rest/notebook-entry/create?notebookId={id}` returns 403 when department or persona does not match.
