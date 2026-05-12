# Sample Test Data for Study Forms

## ARV Initial Entry Form - Sample Data

### Test Case 1: Complete ARV Initial Entry with All Fields

#### Patient Information

```
Subject Number: (Leave empty - not shown for ARV projects)
Site Subject Number (Unique Health ID): 2025-ARV-001
UPID Code (Site Unique Health ID): SITE-HIV-12345
Lab Number: LAB2025001
Gender: Male (select from dropdown)
Date of Birth: 15/03/1985
Age: 40 (auto-calculated, read-only)
```

#### Sample Information

```
Lab Number: LAB2025001 (same as above)
Received Date: 20/01/2025
Received Time: 09:30 AM
Interview Date: 20/01/2025
Interview Time: 10:00 AM
```

#### Project Selection

```
Select Study Form: Initial ARV (radio button)
```

#### ARV Information Section

```
ARV Center Name: Select any center from dropdown (e.g., "Kigali Health Center")
ARV Center Code: Will auto-populate based on center name selection
Doctor / Clinician: Dr. Jean Baptiste
Age (Years): 40 (auto-calculated, read-only)
Under Investigation?: No (select from dropdown)
Under Investigation Notes: (Only shows if "Yes" is selected above)
```

#### Test Selection Section (Specimens Collected)

```
Specimens Collected:
☑ Dry Tube Taken
☑ EDTA Tube Taken

Dry Tube Tests:
☑ Serology HIV Test
☑ Glycemia Test
☑ Creatinine Test
☑ Transaminase Test

EDTA Tube Tests:
☑ CD4 & CD8 Test
☑ CD4 Count Test
☑ CD3 Count Test
☑ NFS Test (Complete Blood Count)
```

---

### Test Case 2: Minimal ARV Initial Entry (Required Fields Only)

#### Patient Information

```
Site Subject Number: 2025-ARV-002
UPID Code: SITE-HIV-12346
Lab Number: LAB2025002
Gender: Female
Date of Birth: 22/07/1992
```

#### Sample Information

```
Received Date: 21/01/2025
Received Time: 11:00 AM
Interview Date: 21/01/2025
Interview Time: 11:15 AM
```

#### Project Selection

```
Select Study Form: Initial ARV
```

#### ARV Information Section

```
ARV Center Name: Select any center
ARV Center Code: Auto-populated
Doctor / Clinician: (Leave empty - optional)
Under Investigation?: No
```

#### Test Selection Section

```
Specimens Collected:
☑ EDTA Tube Taken (at least one required)

EDTA Tube Tests:
☑ CD4 Count Test (at least one test required)
```

---

### Test Case 3: ARV Initial Entry with Under Investigation

#### Patient Information

```
Site Subject Number: 2025-ARV-003
UPID Code: SITE-HIV-12347
Lab Number: LAB2025003
Gender: Male
Date of Birth: 10/11/1978
```

#### Sample Information

```
Received Date: 21/01/2025
Received Time: 02:30 PM
Interview Date: 21/01/2025
Interview Time: 03:00 PM
```

#### Project Selection

```
Select Study Form: Initial ARV
```

#### ARV Information Section

```
ARV Center Name: Select any center
ARV Center Code: Auto-populated
Doctor / Clinician: Dr. Marie Uwase
Under Investigation?: Yes (select from dropdown)
Under Investigation Notes: Patient shows unusual symptoms; further testing required for confirmation of diagnosis. Monitoring liver function.
```

#### Test Selection Section

```
Specimens Collected:
☑ Dry Tube Taken
☑ EDTA Tube Taken

Dry Tube Tests:
☑ Serology HIV Test
☑ Glycemia Test
☑ Creatinine Test
☑ Transaminase Test

EDTA Tube Tests:
☑ CD4 & CD8 Test
☑ CD4 Count Test
☑ NFS Test
```

---

## Expected Backend Request Payload Structure

When you submit the form, the backend should receive a POST request to
`/rest/SampleEntryByProject?type=initial` with the following JSON structure:

```json
{
  "labNo": "LAB2025001",
  "subjectNumber": "",
  "siteSubjectNumber": "2025-ARV-001",
  "upidCode": "SITE-HIV-12345",
  "gender": "M",
  "birthDateForDisplay": "15/03/1985",
  "patientPK": "",
  "patientFhirUuid": "",
  "receivedDateForDisplay": "20/01/2025",
  "receivedTimeForDisplay": "09:30 AM",
  "interviewDate": "20/01/2025",
  "interviewTime": "10:00 AM",
  "currentDate": "21/01/2025",
  "project": "ARV_INITIAL",
  "projectData": {
    "ARVcenterName": "123",
    "ARVcenterCode": "123",
    "doctor": "Dr. Jean Baptiste",
    "dryTubeTaken": true,
    "edtaTubeTaken": true,
    "serologyHIVTest": true,
    "glycemiaTest": true,
    "creatinineTest": true,
    "transaminaseTest": true,
    "cd4cd8Test": true,
    "cd4CountTest": true,
    "cd3CountTest": true,
    "nfsTest": true,
    "underInvestigationNote": ""
  },
  "observations": {
    "nameOfDoctor": "",
    "hivStatus": "",
    "underInvestigation": "456",
    "nameOfRequestor": "",
    "nameOfSampler": ""
  }
}
```

---

## Testing Checklist

### Before Submitting Form

- [ ] ARV Center Name dropdown loads correctly
- [ ] ARV Center Code auto-populates when Center Name is selected
- [ ] Age auto-calculates from Date of Birth
- [ ] Under Investigation Notes field only shows when "Yes" is selected
- [ ] At least one specimen (Dry Tube or EDTA Tube) is checked
- [ ] At least one test is selected

### After Submitting Form

- [ ] Form submission shows success notification
- [ ] Check browser DevTools Network tab for POST request to
      `/rest/SampleEntryByProject?type=initial`
- [ ] Verify request payload matches expected structure
- [ ] Check backend logs for "VL SAMPLE ENTRY DEBUG" section (even for ARV, the
      logging is there)
- [ ] Verify patient is created/updated in database
- [ ] Verify sample is created with correct lab number
- [ ] Verify observations are saved correctly
- [ ] Verify project data is saved correctly
- [ ] Verify test selections are saved

### Backend Verification Queries

```sql
-- Check if patient was created
SELECT * FROM patient WHERE national_id = 'SITE-HIV-12345';

-- Check if sample was created
SELECT * FROM sample WHERE accession_number = 'LAB2025001';

-- Check if observations were saved
SELECT * FROM observation WHERE patient_id = (SELECT id FROM patient WHERE national_id = 'SITE-HIV-12345');

-- Check if project data was saved
SELECT * FROM sample_project WHERE sample_id = (SELECT id FROM sample WHERE accession_number = 'LAB2025001');
```

---

## Common Issues & Troubleshooting

### Issue 1: Form validation fails

**Solution:** Check that all required fields (marked with \*) are filled:

- Lab Number
- Site Subject Number (Unique Health ID)
- UPID Code
- Gender
- Date of Birth
- ARV Center Name
- ARV Center Code
- At least one specimen type
- At least one test selected

### Issue 2: Under Investigation Notes field not showing

**Solution:** Make sure "Yes" is selected in the "Under Investigation?" dropdown

### Issue 3: ARV Center Code not auto-populating

**Solution:**

- Check that ARV organizations are loaded in backend
- Verify organizationLists prop is passed correctly to ARVSection component
- Check browser console for errors

### Issue 4: Backend returns 500 error

**Solution:**

- Check backend logs in console output
- Look for NullPointerException or validation errors
- Verify all required services (PatientService, SampleService, etc.) are
  properly injected
- Check that database tables exist (run Liquibase migrations if needed)

---

## Date Format Notes

- **Display Format:** dd/MM/yyyy (e.g., 15/03/1985)
- **Time Format:** HH:mm AM/PM (e.g., 09:30 AM)
- **Backend expects:** Same format as display (dd/MM/yyyy)

---

## Setting Up ARV Centers in Database

Before testing the ARV form, you need to create ARV centers in the database.

### Option 1: Restart Application (Automatic - Recommended)

The Liquibase migration will run automatically when you restart the Spring Boot
application:

```bash
mvn spring-boot:run -DskipTests -Dmaven.test.skip=true
```

The migration `026-add-arv-organization-type-and-centers.xml` will create:

- ARV Service Loc organization type
- 10 sample ARV centers (Kigali Health Center, Butare Regional Hospital, etc.)

### Option 2: Execute SQL File (Manual)

If the application is already running or you prefer manual setup, use the
provided SQL file:

```bash
# Execute the SQL file directly
docker exec -i openelisglobal-database psql -U clinlims -d clinlims < insert_arv_centers.sql

# OR copy-paste contents from insert_arv_centers.sql into psql terminal
```

The `insert_arv_centers.sql` file in the project root contains:

- Organization type creation
- 10 ARV center records
- Linking of centers to organization type
- Verification query

### Verify ARV Centers Created

```bash
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "SELECT o.id, o.org_id, o.name, o.short_name FROM clinlims.organization o JOIN clinlims.organization_organization_type oot ON o.id = oot.org_id JOIN clinlims.organization_type ot ON oot.org_type_id = ot.id WHERE ot.short_name = 'ARV Service Loc' ORDER BY o.short_name;"
```

Expected output: 10 ARV centers (ARV001 through ARV010) with names like "Kigali
Health Center", "Butare Regional Hospital", etc.

---

## Field Constraints

- **Lab Number:** Max 50 characters, alphanumeric
- **Site Subject Number:** Max 18 characters
- **Subject Number:** Max 7 characters (not shown for ARV)
- **Doctor Name:** Max 50 characters
- **Under Investigation Notes:** Max 1000 characters
- **Date of Birth:** Cannot be in the future
- **Received Date:** Cannot be in the future
- **Interview Date:** Cannot be in the future
