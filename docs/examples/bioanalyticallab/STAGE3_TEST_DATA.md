# Stage 3 (Analytical Execution) - Test Data Examples

This document provides realistic example data that you can use to test the
Analytical Execution workflow, including raw instrument data files and form
values.

---

## 1. Raw Instrument Data Files (For Upload)

### 1.1 LC-MS/MS Chromatogram Data (CSV Format)

**File Name:** `LC-MS-MS_Sample_BIO-2024-001_Analysis.csv`

```csv
Time (min),m/z,Intensity,Scan ID,Retention Time,Peak Area,Peak Height
0.50,235.15,1250,1,0.50,0,0
0.51,235.15,2340,2,0.51,0,0
0.52,235.15,5680,3,0.52,0,0
0.53,235.15,12450,4,0.53,0,0
0.54,235.15,18960,5,0.54,250000,19500
0.55,235.15,22580,6,0.55,620000,23000
0.56,235.15,24100,7,0.56,892000,24500
0.57,235.15,21340,8,0.57,1050000,21500
0.58,235.15,18290,9,0.58,928000,18500
0.59,235.15,12450,10,0.59,580000,12600
0.60,235.15,6780,11,0.60,120000,6900
0.61,235.15,2450,12,0.61,0,2500
1.20,219.08,1100,50,1.20,0,0
1.21,219.08,2100,51,1.21,0,0
1.22,219.08,4560,52,1.22,0,0
1.23,219.08,9870,53,1.23,0,0
1.24,219.08,16340,54,1.24,185000,16500
1.25,219.08,19200,55,1.25,520000,19500
1.26,219.08,20850,56,1.26,750000,21200
1.27,219.08,18900,57,1.27,852000,19100
1.28,219.08,15600,58,1.28,680000,15800
1.29,219.08,10200,59,1.29,380000,10400
1.30,219.08,5340,60,1.30,95000,5500
1.31,219.08,2100,61,1.31,0,2200
```

**What this represents:**

- Time: 0.5-0.61 min = parent drug peak (m/z 235.15)
- Time: 1.2-1.31 min = internal standard peak (m/z 219.08)
- Peak Area: Calculated from the trapezoid rule integration
- Peak Height: Maximum intensity in the peak
- All values are realistic for LC-MS/MS analysis of a bioanalytical sample

---

### 1.2 HPLC UV-Vis Chromatogram Data (CSV Format)

**File Name:** `HPLC-UV_Sample_API-2024-001_Analysis.csv`

```csv
Time (min),Wavelength (nm),Absorbance,Peak Name,Baseline,Area
0.0,254,0.010,Background,0.008,0
0.5,254,0.015,Baseline,0.010,0
1.0,254,0.018,Baseline,0.012,0
1.5,254,0.020,Baseline,0.012,0
2.0,254,0.022,Baseline,0.012,0
2.5,254,0.035,Rising Edge,0.012,2500
3.0,254,0.145,Peak Start,0.012,18000
3.5,254,0.425,Peak Rise,0.012,92000
4.0,254,0.782,Peak Apex,0.012,156000
4.5,254,0.856,Peak Maximum,0.012,185000
5.0,254,0.795,Peak Fall,0.012,156000
5.5,254,0.512,Peak Shoulder,0.012,95000
6.0,254,0.198,Peak End,0.012,32000
6.5,254,0.045,Falling Edge,0.012,4200
7.0,254,0.025,Baseline,0.012,0
7.5,254,0.020,Baseline,0.012,0
8.0,254,0.018,Baseline,0.012,0
```

**What this represents:**

- Time: 3.0-6.5 min = main API peak at 254 nm (typical UV wavelength for
  pharmaceuticals)
- Absorbance: Peak height measured in AU (Absorbance Units)
- Peak Area: Integration of absorbance over time
- Baseline: Instrumental baseline noise
- This is typical HPLC output for pharmaceutical assay

---

### 1.3 Dissolution Data (CSV Format)

**File Name:** `Dissolution_Tablet_Sample_PHM-2024-001_Analysis.csv`

```csv
Time (min),Vessel,Temperature (C),% Release,Mean,Std Dev,RSD (%)
5,1,37.0,18.5,18.2,1.2,6.6
5,2,37.1,17.8,18.2,1.2,6.6
5,3,37.0,19.2,18.2,1.2,6.6
5,4,37.1,17.4,18.2,1.2,6.6
5,5,37.0,18.6,18.2,1.2,6.6
5,6,37.0,18.3,18.2,1.2,6.6
10,1,37.0,42.3,42.1,1.8,4.3
10,2,37.1,40.8,42.1,1.8,4.3
10,3,37.0,43.5,42.1,1.8,4.3
10,4,37.1,41.9,42.1,1.8,4.3
10,5,37.0,42.7,42.1,1.8,4.3
10,6,37.0,41.8,42.1,1.8,4.3
15,1,37.0,65.2,65.8,2.1,3.2
15,2,37.1,64.5,65.8,2.1,3.2
15,3,37.0,67.3,65.8,2.1,3.2
15,4,37.1,65.6,65.8,2.1,3.2
15,5,37.0,66.2,65.8,2.1,3.2
15,6,37.0,65.9,65.8,2.1,3.2
30,1,37.0,91.2,91.5,2.3,2.5
30,2,37.1,89.8,91.5,2.3,2.5
30,3,37.0,93.4,91.5,2.3,2.5
30,4,37.1,91.5,91.5,2.3,2.5
30,5,37.0,91.8,91.5,2.3,2.5
30,6,37.0,91.3,91.5,2.3,2.5
45,1,37.0,98.5,98.2,1.5,1.5
45,2,37.1,97.8,98.2,1.5,1.5
45,3,37.0,98.9,98.2,1.5,1.5
45,4,37.1,98.1,98.2,1.5,1.5
45,5,37.0,98.3,98.2,1.5,1.5
45,6,37.0,98.2,98.2,1.5,1.5
```

**What this represents:**

- Time points: 5, 10, 15, 30, 45 minutes (typical USP dissolution timepoints)
- 6 vessels per timepoint (USP requirement)
- % Release: Percentage of drug released at each timepoint
- Mean/Std Dev/RSD: Statistical calculations for tablet consistency
- Temperature: Maintained at 37°C ± 0.5°C (USP requirement)
- Shows typical S-shaped dissolution profile (slow start, rapid middle, plateau
  at end)

---

### 1.4 Calibration Curve Data (CSV Format)

**File Name:** `Calibration_Curve_LC-MS-MS_Date_2026-01-07.csv`

```csv
Concentration (ng/mL),Replicate 1,Replicate 2,Replicate 3,Mean,Std Dev,CV (%)
1.0,8950,9120,8850,8973,135,1.51
2.0,18340,18890,17950,18393,468,2.55
5.0,45600,47200,44800,45867,1200,2.62
10.0,92100,95300,90800,92733,2251,2.43
25.0,225000,234500,221000,226833,6708,2.96
50.0,452000,468900,442100,454333,13675,3.01
100.0,905000,938000,890000,911000,24413,2.68
250.0,2245000,2315000,2180000,2246667,67500,3.01
500.0,4490000,4625000,4410000,4508333,108012,2.40
1000.0,8950000,9235000,8750000,8945000,242583,2.71
```

**What this represents:**

- Concentration range: 1-1000 ng/mL (typical for bioanalytical assays)
- 3 replicates per concentration
- Peak area responses (in AU or counts)
- CV (Coefficient of Variation): All < 5% (excellent reproducibility)
- Data follows linear relationship (good for regression)

---

## 2. Form Values for Setup Instrumentation

### LC-MS/MS Setup

```json
{
  "analystId": "ANALYST_001",
  "analystName": "John Smith",
  "instrumentId": "1",
  "instrumentName": "LC-MS/MS System",
  "batchNumber": "BATCH-2026-001",
  "executionDate": "2026-01-07",
  "executionTime": "09:30",
  "testParameters": {
    "ionizationMode": "ESI-Positive",
    "scanType": "MRM (Multiple Reaction Monitoring)",
    "parentIon": "235.15",
    "fragmentIon": "162.10",
    "collisionEnergy": "35 eV",
    "declustering": "80 V",
    "sourceTemp": "450°C",
    "columnTemp": "40°C",
    "flowRate": "0.5 mL/min",
    "injectionVolume": "10 µL",
    "runtime": "8 minutes",
    "mobilePhasA": "0.1% Formic Acid in Water",
    "mobilePhasB": "0.1% Formic Acid in Acetonitrile",
    "gradientProfile": "5% B (0 min) → 95% B (5 min) → 5% B (8 min)"
  },
  "notes": "Standard LC-MS/MS method for bioavailability study"
}
```

### HPLC UV-Vis Setup

```json
{
  "analystId": "ANALYST_002",
  "analystName": "Jane Doe",
  "instrumentId": "2",
  "instrumentName": "HPLC System",
  "batchNumber": "BATCH-2026-002",
  "executionDate": "2026-01-07",
  "executionTime": "10:15",
  "testParameters": {
    "detectorType": "UV-Vis",
    "wavelength": "254 nm",
    "detectionMode": "Absorbance",
    "columnType": "C18 Reversed-Phase",
    "columnLength": "250 mm",
    "columnDiameter": "4.6 mm",
    "particleSize": "5 µm",
    "columnTemp": "25°C",
    "flowRate": "1.0 mL/min",
    "injectionVolume": "20 µL",
    "runtime": "15 minutes",
    "mobilePhasA": "0.05 M Potassium Phosphate Buffer pH 3.0",
    "mobilePhasB": "Acetonitrile",
    "gradientProfile": "10% B (0 min) → 50% B (10 min) → 10% B (15 min)",
    "sampleTemperature": "4°C"
  },
  "notes": "HPLC method for pharmaceutical assay per USP <621>"
}
```

### Dissolution Tester Setup

```json
{
  "analystId": "ANALYST_003",
  "analystName": "Mike Johnson",
  "instrumentId": "3",
  "instrumentName": "Dissolution Apparatus II",
  "batchNumber": "BATCH-2026-003",
  "executionDate": "2026-01-07",
  "executionTime": "08:00",
  "testParameters": {
    "apparatus": "USP Apparatus II (Paddle)",
    "mediumType": "Simulated Gastric Fluid (SGF)",
    "mediumPH": "1.2",
    "mediumVolume": "900 mL",
    "temperature": "37.0°C",
    "toleranceTemp": "±0.5°C",
    "rpm": "75",
    "paddelDistance": "25 mm",
    "sampleSize": "1 Tablet",
    "samplingTimepoints": "5, 10, 15, 30, 45 minutes",
    "sampleVolume": "10 mL",
    "filterType": "0.45 µm PTFE",
    "detectionMethod": "HPLC at 254 nm",
    "vessels": "6"
  },
  "notes": "USP <711> Dissolution Test for tablet formulation"
}
```

---

## 3. Calibration Data Entry Form

### Calibration Curve Validation Values

```json
{
  "calibrationDate": "2026-01-07",
  "calibrationTime": "08:00",
  "standardLot": "STD-123456",
  "standardExpiration": "2026-06-30",
  "internalStandardLot": "IS-789012",
  "regressionType": "Linear (1/X weighting)",
  "concentrationRange": "1-1000 ng/mL",
  "numPoints": 10,
  "numReplicates": 3,
  "rSquared": 0.9987,
  "slope": 8945.23,
  "slopeStdError": 125.34,
  "intercept": 5234.12,
  "interceptStdError": 892.45,
  "residualStdDev": 45230,
  "percentChange": 1.23,
  "acceptanceStatus": "PASS",
  "notes": "Calibration curve meets all acceptance criteria. Ready for sample analysis."
}
```

---

## 4. QC Sample Results Data Entry

### Low QC (5 ng/mL) Results

```json
{
  "qcLevel": "LOW",
  "targetConcentration": 5,
  "replicate1": {
    "measuredConcentration": 4.92,
    "accuracy": 98.4,
    "peakArea": 45120,
    "retentionTime": 5.23
  },
  "replicate2": {
    "measuredConcentration": 5.08,
    "accuracy": 101.6,
    "peakArea": 45890,
    "retentionTime": 5.24
  },
  "replicate3": {
    "measuredConcentration": 5.01,
    "accuracy": 100.2,
    "peakArea": 45450,
    "retentionTime": 5.23
  },
  "mean": 5.0,
  "accuracy": 100.1,
  "stdDev": 0.081,
  "cv": 1.62,
  "acceptanceStatus": "PASS",
  "acceptanceCriteria": {
    "accuracy": "80-120%",
    "cv": "≤20%"
  }
}
```

### Medium QC (50 ng/mL) Results

```json
{
  "qcLevel": "MEDIUM",
  "targetConcentration": 50,
  "replicate1": {
    "measuredConcentration": 49.15,
    "accuracy": 98.3,
    "peakArea": 451200,
    "retentionTime": 5.23
  },
  "replicate2": {
    "measuredConcentration": 50.82,
    "accuracy": 101.6,
    "peakArea": 458900,
    "retentionTime": 5.24
  },
  "replicate3": {
    "measuredConcentration": 50.13,
    "accuracy": 100.3,
    "peakArea": 454500,
    "retentionTime": 5.23
  },
  "mean": 50.03,
  "accuracy": 100.1,
  "stdDev": 0.843,
  "cv": 1.68,
  "acceptanceStatus": "PASS",
  "acceptanceCriteria": {
    "accuracy": "80-120%",
    "cv": "≤15%"
  }
}
```

### High QC (500 ng/mL) Results

```json
{
  "qcLevel": "HIGH",
  "targetConcentration": 500,
  "replicate1": {
    "measuredConcentration": 492.34,
    "accuracy": 98.5,
    "peakArea": 4512000,
    "retentionTime": 5.23
  },
  "replicate2": {
    "measuredConcentration": 508.21,
    "accuracy": 101.6,
    "peakArea": 4589000,
    "retentionTime": 5.24
  },
  "replicate3": {
    "measuredConcentration": 501.45,
    "accuracy": 100.3,
    "peakArea": 4545000,
    "retentionTime": 5.23
  },
  "mean": 500.67,
  "accuracy": 100.1,
  "stdDev": 7.92,
  "cv": 1.58,
  "acceptanceStatus": "PASS",
  "acceptanceCriteria": {
    "accuracy": "80-120%",
    "cv": "≤15%"
  }
}
```

---

## 5. Sample Analysis Results Data Entry

### Sample Result 1: BIO-2024-001 (Plasma Sample)

```json
{
  "sampleId": "BIO-2024-001",
  "analyticalMethod": "LC_MS_MS",
  "testDate": "2026-01-07",
  "analystId": "ANALYST_001",
  "instrumentId": "1",
  "batchNumber": "BATCH-2026-001",
  "sampleType": "Plasma",
  "results": {
    "concentration": 247.5,
    "unit": "ng/mL",
    "accuracy": 98.5,
    "precision": 2.1,
    "detectionMethod": "MRM 235.15→162.10",
    "retentionTime": 5.23,
    "peakArea": 2245000,
    "peakHeight": 185000,
    "signalToNoise": 125
  },
  "qcStatus": "PASS",
  "westgardResult": "PASS",
  "dataIntegrity": {
    "originalData": "LC-MS-MS_Sample_BIO-2024-001_Analysis.csv",
    "checksumVerified": true,
    "timestampVerified": true,
    "originalityVerified": true
  },
  "alcoapCompliance": {
    "attributable": "ANALYST_001 on 2026-01-07 09:30",
    "legible": true,
    "contemporaneous": true,
    "original": true,
    "accurate": true,
    "complete": true
  },
  "approvalStatus": "PENDING_ANALYST_REVIEW",
  "notes": "Sample analyzed within 48 hours of receipt. QC all within limits."
}
```

### Sample Result 2: API-2024-001 (Pharmaceutical API)

```json
{
  "sampleId": "API-2024-001",
  "analyticalMethod": "HPLC_UV_VIS",
  "testDate": "2026-01-07",
  "analystId": "ANALYST_002",
  "instrumentId": "2",
  "batchNumber": "BATCH-2026-002",
  "sampleType": "API",
  "results": {
    "assay": 99.8,
    "unit": "%",
    "specification": "98.0-101.0%",
    "purity": 99.8,
    "relatedSubstance": 0.2,
    "waterContent": 0.15,
    "retentionTime": 4.23,
    "peakArea": 2246667,
    "resolution": 1.8,
    "theoreticalPlates": 8950
  },
  "qcStatus": "PASS",
  "westgardResult": "PASS",
  "dataIntegrity": {
    "originalData": "HPLC-UV_Sample_API-2024-001_Analysis.csv",
    "checksumVerified": true,
    "timestampVerified": true,
    "originalityVerified": true
  },
  "alcoapCompliance": {
    "attributable": "ANALYST_002 on 2026-01-07 10:15",
    "legible": true,
    "contemporaneous": true,
    "original": true,
    "accurate": true,
    "complete": true
  },
  "approvalStatus": "PENDING_ANALYST_REVIEW",
  "notes": "API meets all specification limits. Purity excellent at 99.8%."
}
```

### Sample Result 3: PHM-2024-001 (Tablet Dissolution)

```json
{
  "sampleId": "PHM-2024-001",
  "analyticalMethod": "DISSOLUTION_USP",
  "testDate": "2026-01-07",
  "analystId": "ANALYST_003",
  "instrumentId": "3",
  "batchNumber": "BATCH-2026-003",
  "sampleType": "Tablet",
  "results": {
    "timepoint5": {
      "mean": 18.2,
      "std": 1.2,
      "rsd": 6.6,
      "specification": "NMT 35%"
    },
    "timepoint10": {
      "mean": 42.1,
      "std": 1.8,
      "rsd": 4.3,
      "specification": "NMT 65%"
    },
    "timepoint15": {
      "mean": 65.8,
      "std": 2.1,
      "rsd": 3.2,
      "specification": "75-105%"
    },
    "timepoint30": {
      "mean": 91.5,
      "std": 2.3,
      "rsd": 2.5,
      "specification": "NLT 75%"
    },
    "timepoint45": {
      "mean": 98.2,
      "std": 1.5,
      "rsd": 1.5,
      "specification": "NLT 75%"
    }
  },
  "profile": "S-shaped (normal)",
  "qcStatus": "PASS",
  "westgardResult": "PASS",
  "dataIntegrity": {
    "originalData": "Dissolution_Tablet_Sample_PHM-2024-001_Analysis.csv",
    "checksumVerified": true,
    "temperatureLog": "37.0 ± 0.3°C maintained",
    "vestals": 6
  },
  "alcoapCompliance": {
    "attributable": "ANALYST_003 on 2026-01-07 08:00",
    "legible": true,
    "contemporaneous": true,
    "original": true,
    "accurate": true,
    "complete": true
  },
  "approvalStatus": "PENDING_ANALYST_REVIEW",
  "notes": "Dissolution profile acceptable. All timepoints within specification. RSD excellent."
}
```

---

## 6. Deviation Documentation Example

```json
{
  "deviationId": "DEV-2026-001",
  "reportDate": "2026-01-07",
  "reportedBy": "ANALYST_001",
  "deviationType": "INSTRUMENT",
  "severity": "MINOR",
  "description": "LC-MS/MS source temperature drifted to 455°C instead of 450°C during Sample 3 analysis",
  "rootCause": "Thermocouple calibration drift",
  "impact": "Potential 2-3% variation in ionization efficiency for that sample",
  "correctiveAction": "Recalibrated source temperature. Re-analyzed Sample 3. Results within 1% of original.",
  "preventiveMeasure": "Scheduled monthly thermocouple calibration verification",
  "approvedBy": "ANALYST_SUPERVISOR",
  "approvalDate": "2026-01-07",
  "status": "CLOSED"
}
```

---

## 7. Multi-Level Review Data Entry

### Analyst Review Form

```json
{
  "reviewType": "ANALYST_REVIEW",
  "sampleId": "BIO-2024-001",
  "reviewerId": "ANALYST_001",
  "reviewerName": "John Smith",
  "reviewDate": "2026-01-07",
  "reviewTime": "14:30",
  "comments": "Chromatography clean, no interfering peaks. QC all acceptable. Calibration curve excellent (R²=0.9987). Data integrity verified. Ready for QA review.",
  "checklistItems": {
    "calibrationCurveAcceptable": true,
    "qcWithinLimits": true,
    "noChromArtifacts": true,
    "retentionTimeConsistent": true,
    "peakIntegrityGood": true,
    "dataIntegrityVerified": true,
    "alcoapCompliant": true
  },
  "approved": true,
  "electronicSignature": "jsm1501_2026-01-07_14:32",
  "certificateVerified": true
}
```

### QA/Senior Scientist Review Form

```json
{
  "reviewType": "QA_REVIEW",
  "sampleId": "BIO-2024-001",
  "reviewerId": "QA_SCIENTIST_001",
  "reviewerName": "Dr. Sarah Mitchell",
  "reviewDate": "2026-01-08",
  "reviewTime": "09:15",
  "comments": "Analyst review thorough. Method validation reviewed - all parameters within control limits. HPLC system suitability excellent. Sample preparation documented adequately. No GLP deviations noted. Recommend approval pending manager sign-off.",
  "checklistItems": {
    "methodValidation": true,
    "systemSuitability": true,
    "samplePreparationCorrect": true,
    "glpCompliance": true,
    "noUnresolvedDeviations": true,
    "dataQualityAcceptable": true,
    "regulatoryCompliance": true,
    "auditTrailIntact": true
  },
  "dataIntegrityVerified": true,
  "methodComplianceVerified": true,
  "qcAcceptable": true,
  "approved": true,
  "electronicSignature": "smitchell_qa_2026-01-08_09:18",
  "certificateVerified": true
}
```

### Final Manager/Study Director Approval

```json
{
  "reviewType": "MANAGER_APPROVAL",
  "sampleId": "BIO-2024-001",
  "reviewerId": "LAB_MANAGER_001",
  "reviewerName": "Dr. James Wilson",
  "reviewDate": "2026-01-08",
  "reviewTime": "15:00",
  "comments": "Final approval granted. Data integrity audit trail reviewed - all checks passed. Regulatory compliance confirmed for 21 CFR Part 11. Results acceptable for submission to study sponsor.",
  "checklistItems": {
    "regulatoryCompliance": true,
    "studyImpactReviewed": true,
    "auditTrailComplete": true,
    "finalDispositionDetermined": "RELEASE_TO_SPONSOR"
  },
  "regulatoryCompliance": true,
  "studyImpact": "Critical sample for pharmacokinetic profile. Results support continuation of bioequivalence study.",
  "finalDisposition": "RELEASE_TO_SPONSOR",
  "approved": true,
  "electronicSignature": "jwilson_mgr_2026-01-08_15:03",
  "certificateVerified": true
}
```

---

## 8. How to Use This Data in Stage 3

### Step-by-Step Instructions:

1. **Setup Instrumentation Tab:**

   - Copy values from sections 2.1-2.3 (Setup values)
   - Fill in form fields for your selected analytical method
   - Save instrument configuration

2. **Upload Raw Data:**

   - Create a text file with data from sections 1.1-1.4
   - Save as `.csv` file with appropriate naming
   - Use the FileUploader in Stage 3 form
   - Select appropriate instrument type (LC-MS/MS, HPLC, etc.)

3. **Enter Calibration Data:**

   - Use values from section 3 (Calibration Data)
   - Enter R² value: 0.9987
   - Enter slope: 8945.23
   - Enter intercept: 5234.12
   - Verify all acceptance criteria are met

4. **Enter QC Results:**

   - Use data from section 4 (QC Sample Results)
   - Enter results for Low, Medium, and High QC samples
   - All should show PASS status
   - Verify accuracy and CV within limits

5. **Enter Sample Analysis Results:**

   - Use data from section 5 (Sample Analysis Results)
   - Enter concentration: 247.5 ng/mL for BIO-2024-001
   - Enter assay: 99.8% for API-2024-001
   - All should show PASS for QC status

6. **Document Deviations (if any):**

   - Use template from section 6
   - Document any out-of-spec conditions
   - Provide corrective and preventive actions

7. **Complete Multi-Level Reviews:**
   - Analyst review: Approve after verifying data quality
   - QA review: Approve after checking methodology
   - Manager approval: Final sign-off for release to sponsor

---

## 9. Validation Criteria Checklist

### Data Must Pass:

- ✅ Calibration curve R² ≥ 0.99
- ✅ Calibration accuracy 80-120%
- ✅ QC accuracy 80-120% (or 85-115% for LLOQ)
- ✅ QC CV ≤ 15% (or ≤ 20% for LLOQ)
- ✅ Dissolution RSD ≤ 6% (or ≤ 10% for first 2 vessels)
- ✅ No Westgard rule violations
- ✅ ALCOA+ compliance verified
- ✅ All deviations documented and approved
- ✅ All three levels of review completed

---

## 10. Typical Workflow Timeline

```
08:00 - Analyst: Setup instrumentation, start analysis
08:15 - System: Injecting QC/calibration standards
09:00 - System: Analyzing study samples
10:00 - Analyst: Complete raw data file upload
11:00 - Analyst: Enter calibration and QC results
12:00 - Analyst: Enter sample analysis results
13:00 - Analyst: Complete and submit analyst review
14:00 - QA Scientist: Review and approve data
15:00 - Lab Manager: Final approval and release
15:15 - System: Results released to study sponsor
```
