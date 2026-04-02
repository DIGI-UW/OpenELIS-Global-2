# Patient Reports Summary - OpenELIS Global 2

This report provides a comprehensive overview of all patient-related reports
available in the OpenELIS Global 2 reporting module. It includes the total
count, usage categories, implementation details, and an analysis of query
complexity.

## 1. Executive Summary

- **Total Patient Reports Identified:** 24
- **Primary Technology:** Java (Spring MVC) + JasperReports (.jrxml)
- **Data Fetching:** Hybrid approach using HQL (Hibernate) for individual
  reports and Raw SQL (PostgreSQL) for bulk data exports.

---

## 2. Report Classification & Usage

| Category               | Reports Included                                                                          | Primary Usage                                                                                 |
| :--------------------- | :---------------------------------------------------------------------------------------- | :-------------------------------------------------------------------------------------------- |
| **Diagnostic Results** | Patient Clinical, Pathology, Cytology, Immuno-Chemistry, Breast Cancer (Hormone Receptor) | Delivery of lab results to patients and healthcare providers.                                 |
| **HIV/ART Monitoring** | Patient ARV (Initial/Follow-up), Viral Load (VL), Early Infant Diagnosis (EID)            | Longitudinal tracking of HIV/AIDS patients across clinical workflows.                         |
| **TB Monitoring**      | TB Patient Report                                                                         | Tracking Tuberculosis diagnostics and treatment monitoring.                                   |
| **Inconclusive/QC**    | Patient Indeterminate, Non-Conformity Notifications                                       | Management of samples requiring re-testing or clinical follow-up due to inconclusive results. |
| **Collection & IDs**   | Patient Collection Report, Patient Associated Report                                      | Internal laboratory tracking of patient-specific sample associations.                         |

---

## 3. Detailed Report Inventory

| Implementation Class          | Purpose / Usage                                            | Query Complexity |
| :---------------------------- | :--------------------------------------------------------- | :--------------- |
| `PatientClinicalReport`       | Standard diagnostic report for general chemistry/serology. | Medium           |
| `PatientARVReport`            | Detailed HIV monitoring including historical results.      | Very High        |
| `PatientVLVersion1Report`     | Viral load monitoring with log-scale conversion.           | High             |
| `PatientEIDVersion1Report`    | Early Infant Diagnosis (PCR-based HIV testing).            | High             |
| `TBPatientReport`             | Comprehensive reporting for TB smear and culture.          | Medium-High      |
| `PatientPathologyReport`      | Tissue-based diagnostic reporting.                         | Medium           |
| `PatientCytologyReport`       | Cell-based diagnostic reporting.                           | Medium           |
| `PatientIndeterminateReport`  | Tracking indeterminate HIV/Clinical results.               | Medium           |
| `PatientSpecialRequestReport` | Ad-hoc patient-specific requests.                          | Low              |
| `RetroCIPatientCollection`    | Tracking retroactive patient sample collections.           | Low              |

---

## 4. Query Complexity Ranking (Hardcoded Queries)

Based on the analysis of the `ColumnBuilder` and `Implementation` classes, here
are the hardcoded queries ranked by complexity (**More complex first**):

### 1. Longitudinal HIV/ARV Exports (`ARVColumnBuilder.java`)

- **Complexity:** **Extremely High**
- **Rationale:** Uses advanced PostgreSQL `crosstab()` functions. Joins **8+
  tables** including `observation_history` for multiple clinical values (prior
  treatments, diseases) and pivots them into a single CSV row. Uses regex
  filters for data sanitization.

### 2. Viral Load / EID Data Builders (`VLColumnBuilder.java`, `EIDColumnBuilder.java`)

- **Complexity:** **High**
- **Rationale:** Similar to ARV, these use pivots but with fewer secondary
  clinical joins. They involve complex mathematical transformations (Log
  calculations) directly in the data-pipe.

### 3. Patient Demographic Mapping (`CIColumnBuilder.java`)

- **Complexity:** **Medium-High**
- **Rationale:** Standardized join across `patient`, `person`, `sample_human`,
  `sample_projects`, and `organization`. Ensures sample-to-project-to-site
  integrity.

### 4. Diagnostic Result Fetching (`PatientReport.java`)

- **Complexity:** **Medium**
- **Rationale:** Primarily uses HQL (Hibernate) for individual `SampleID`
  fetching. Joins `Result` and `Analyte` tables but remains optimized for
  single-patient retrieval.

---

## 5. Architectural Implementation Notes

### Frontend Stack (UI)

- **Framework:** React 17
- **Design System:** Carbon Design System (IBM)
- **Internationalization:** Mandatory via `react-intl`. No hardcoded English
  strings allowed.
- **Data Management:** SWR for caching and `Formik/Yup` for validation.

### Backend Stack (API)

- **Language:** Java 21
- **Framework:** Spring Framework 6.2.2 (MVC)
- **Persistence:** Hibernate 6.x (Jakarta EE 9)
- **Database:** PostgreSQL 14+
- **Reporting Engine:** JasperReports (JasperReports 6.17+)
