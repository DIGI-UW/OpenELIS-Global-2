cat > /tmp/arv_vl_test_data.md << 'EOF'
# ARV - VIRAL LOAD FORM TEST DATA

## How to Test:
1. Go to: http://localhost:3000/StudyInitialEntry (or http://localhost:8080/SampleEntryByProject for old UI)
2. Select Project: **"ARV - Viral Load"**
3. Fill the form with data below
4. Click **Save**
5. Check backend terminal for debug logs

---

## STEP 1: Patient Information

**Lab Number:** (Auto-generated, e.g., LVL00001)
**Subject Number:** 1234567
**Site Subject Number:** ABC-12-00001
**Date of Birth:** 01/01/1985
**Age:** 40 years
**Gender:** Male (or Female to test pregnancy fields)

**Received Date:** 10/02/2026
**Received Time:** 10:30
**Interview Date:** 10/02/2026
**Interview Time:** 09:00

---

## STEP 2: VL Section Fields

### Organization
- **ARV Center Name:** Select any center from dropdown (e.g., "Center 1" or first option)
- **ARV Center Code:** Will auto-populate based on center name

### Clinician/Sampler
- **Name of Clinician:** Dr. John Smith
- **Name of Sampler:** Nurse Jane Doe

### Patient Info (Female only - will show if Gender = Female)
- **Pregnant?:** Yes
- **Breastfeeding?:** No

### HIV Status
- **HIV Type:** HIV-1 *(REQUIRED)*

### Under Investigation
- **Under Investigation?:** Yes
- **Investigation Notes:** Testing VL section implementation with new React UI

---

## STEP 3: ARV Treatment

### Currently on ARV Treatment
- **Currently on ARV Treatment?:** Yes

### If Yes, these fields appear:
- **ARV Treatment Start Date:** 01/01/2024
- **Therapeutic Line:** First Line (or select from dropdown)
- **Current ARV Regimen:** AZT+3TC+EFV

---

## STEP 4: VL Request Details

### Reason for VL Request
- **Reason for VL Request:** Routine monitoring (or select from dropdown)
- **Specify Other Reason:** (Only if "Other" selected above) - Leave blank for now

---

## STEP 5: CD4 Counts

### CD4 Initial
- **CD4 Count:** 350
- **CD4 Percent:** 25
- **CD4 Date:** 01/12/2025

### CD4 at Demand
- **CD4 Count:** 400
- **CD4 Percent:** 28
- **CD4 Date:** 05/02/2026

---

## STEP 6: Prior VL Information

### Prior VL Request
- **Prior VL Request?:** Yes

### If Yes, these fields appear:
- **Prior VL Lab:** Central Reference Lab
- **Prior VL Value:** 5000
- **Prior VL Date:** 01/12/2025

---

## STEP 7: Specimens & Tests

### Specimens Collected (Check all that apply)
- ☑ **EDTA Tube Taken**
- ☑ **DBS (Dry Blood Spot)**
- ☐ PSC

### Tests (Check all that apply)
- ☑ **Viral Load Test** *(REQUIRED for VL project)*
- ☑ **CD4/CD8 Test**
- ☑ **CD4 Count Test**
- ☐ CD3 Count Test

### Biochemistry Tests
- ☑ **Glycemia Test**
- ☑ **Creatinine Test**
- ☐ Transaminase Test
- ☐ ALT (SGPT) Test
- ☐ AST (SGOT) Test

### Hematology Tests (NFS)
- ☑ **NFS (Full Blood Count)**
- ☐ GB (White Blood Cells)
- ☐ Neutrophils
- ☐ Lymphocytes
- ☐ HB (Hemoglobin)
- ☐ HCT (Hematocrit)
- ☐ PLQ (Platelets)

---

## EXPECTED BACKEND LOG OUTPUT:

After clicking Save, you should see in backend terminal:

```
========== VL SAMPLE ENTRY DEBUG ==========
Lab Number: LVL00001
Project: ARV_VIRAL_LOAD
Patient PK: (some ID)
Subject Number: 1234567
Site Subject Number: ABC-12-00001
Gender: M
Birth Date: 01/01/1985

--- ProjectData ---
ARVcenterName: 1
ARVcenterCode: 2
underInvestigationNote: Testing VL section implementation with new React UI
edtaTubeTaken: true
dbsvlTaken: true
pscvlTaken: false
viralLoadTest: true
cd4cd8Test: true
cd4CountTest: true
cd3CountTest: false
glycemiaTest: true
creatinineTest: true
transaminaseTest: false
nfsTest: true

--- Observations ---
nameOfDoctor: Dr. John Smith
nameOfSampler: Nurse Jane Doe
hivStatus: (dictionary ID)
underInvestigation: (dictionary ID for Yes)
vlPregnancy: null (or ID if Female)
vlSuckle: null (or ID if Female)
currentARVTreatment: (dictionary ID for Yes)
arvTreatmentInitDate: 01/01/2024
arvTreatmentRegime: (dictionary ID)
currentARVTreatmentINNsList: [AZT+3TC+EFV]
vlReasonForRequest: (dictionary ID)
vlOtherReasonForRequest: null
initcd4Count: 350
initcd4Percent: 25
initcd4Date: 01/12/2025
demandcd4Count: 400
demandcd4Percent: 28
demandcd4Date: 05/02/2026
vlBenefit: (dictionary ID for Yes)
priorVLLab: Central Reference Lab
priorVLValue: 5000
priorVLDate: 01/12/2025
==========================================

========== DATABASE SAVE VERIFICATION ==========
✓ Sample saved successfully!
✓ Lab Number: LVL00001
✓ Project: ARV_VIRAL_LOAD

To verify in database, run:
  SELECT * FROM sample WHERE accession_number = 'LVL00001';
================================================
```

---

## QUICK MINIMAL TEST (If short on time)

Just fill these REQUIRED fields:
1. **Gender:** Male
2. **ARV Center Name:** Any
3. **HIV Type:** HIV-1
4. **Under Investigation:** Yes → "Test save"
5. **Check boxes:** EDTA Tube + Viral Load Test
6. **Click Save**

This minimal data should trigger the save and show logs!

---

## DATABASE VERIFICATION

After saving, run in psql:

```sql
-- Check sample created
SELECT accession_number, entered_date 
FROM sample 
WHERE accession_number LIKE 'LVL%' 
ORDER BY entered_date DESC 
LIMIT 1;

-- Check observations saved
SELECT oht.type_name, oh.value
FROM observation_history oh
JOIN observation_history_type oht ON oh.observation_history_type_id = oht.id
WHERE oh.patient_id = (
    SELECT patient_id FROM sample WHERE accession_number = 'LVL00001'
)
ORDER BY oht.type_name;
```

---

## TROUBLESHOOTING

**If Save fails:**
- Check browser Console (F12) for errors
- Check Network tab → POST request to `/rest/SampleEntryByProject`
- Check Request Payload structure

**If Backend doesn't show logs:**
- Make sure backend is running with your updated code
- Check terminal where backend is running
- Logs appear BEFORE save completes

EOF
cat /tmp/arv_vl_test_data.md