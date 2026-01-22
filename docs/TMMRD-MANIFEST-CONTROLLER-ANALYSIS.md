# TMMRD Manifest Controller Analysis & Gap Report

This document compares the TMMRD manifest implementation with reference
implementations from Immunology, Bioanalytical, and Bioequivalence labs to
identify gaps and recommend improvements.

---

## Executive Summary

**Status:** TMMRD has a functional but incomplete manifest controller.

**What's Implemented:**

- ✅ POST `/rest/notebook/tradmed/entry/{entryId}/samples/preview-manifest` -
  CSV preview and validation
- ✅ POST
  `/rest/notebook/tradmed/entry/{entryId}/samples/create-from-manifest` - CSV
  import and sample creation
- ✅ CSV parsing with column mapping support
- ✅ Row-level and field-level validation
- ✅ Support for batch creation (multiple samples per row via `numOfSamples`)

**What's Missing:**

- ❌ GET `/rest/notebook/tradmed/sample-categories` - Frontend expects this
  endpoint to fetch valid sample categories
- ❌ Pagination support (Bioanalytical has this)
- ❌ Filtering support (Bioanalytical has this)

---

## Controller Comparison Matrix

### TMMRD ManifestImportController

**File:**
`src/main/java/org/openelisglobal/notebook/controller/rest/TraditionalMedicineManifestImportController.java`

**Base Path:** `/rest/notebook/tradmed`

**Endpoints:**

```
POST   /entry/{entryId}/samples/preview-manifest
POST   /entry/{entryId}/samples/create-from-manifest
```

**Key Features:**

- Supports batch creation (multiple samples per CSV row via `numOfSamples`
  field)
- Calculates total samples to create (`totalSamplesToCreate` = sum of
  `numOfSamples`)
- Returns created accession numbers in response
- Validates all rows before importing (all-or-nothing approach)

**Service Class:**

- `TraditionalMedicineManifestImportService`
- Methods: `parseManifestCsv()`, `validateSampleData()`,
  `createSamplesForEntry()`
- Result type: `TraditionalMedicineManifestImportResult`

---

### Immunology ManifestImportController (Reference)

**File:**
`src/main/java/org/openelisglobal/notebook/controller/rest/ImmunologyManifestImportController.java`

**Base Path:** `/rest/notebook/immunology`

**Endpoints:**

```
GET    /sample-types
POST   /entry/{entryId}/samples/preview-manifest
POST   /entry/{entryId}/samples/create-from-manifest
```

**Key Features:**

- GET endpoint returns list of valid sample types
- No pagination (simple list response)
- 1 sample per CSV row (no batching)
- Returns created accession numbers

**Response Format (GET /sample-types):**

```json
{
  "sampleTypes": [
    { "id": "...", "description": "..." },
    ...
  ],
  "total": 15
}
```

---

### Bioanalytical ManifestImportController (Reference)

**File:**
`src/main/java/org/openelisglobal/notebook/controller/rest/BioanalyticalManifestImportController.java`

**Base Path:** `/rest/notebook/bioanalytical` (serves both Bioanalytical &
Bioequivalence labs)

**Endpoints:**

```
GET    /sample-types?offset=0&limit=50&filter=optional
GET    /tests?offset=0&limit=50&filter=optional
GET    /source-origins?offset=0&limit=50&filter=optional
POST   /entry/{entryId}/samples/preview-manifest
POST   /entry/{entryId}/samples/import-manifest
```

**Key Features:**

- 3 GET endpoints for different validated lists
- Pagination support (offset/limit, max 200 records per request)
- Optional filtering by description or ID
- Validation: offset >= 0, 1 <= limit <= 200
- POST endpoint named `import-manifest` (not `create-from-manifest`)
- 1 sample per CSV row (no batching)
- Returns created sample IDs

**Response Format (GET /sample-types):**

```json
{
  "sampleTypes": [
    { "id": "...", "description": "..." },
    ...
  ],
  "total": 125,
  "offset": 0,
  "limit": 50
}
```

---

## Critical Gap Analysis

### Gap 1: Missing Sample Categories Endpoint

**Impact Level:** HIGH 🔴

**Frontend Dependency:** The manifest modal frontend code
(TraditionalMedicineManifestImportModal.js) explicitly fetches sample
categories:

```javascript
useEffect(() => {
  if (open) {
    fetch(`${config.serverBaseUrl}/rest/notebook/tradmed/sample-categories`, {
      method: "GET",
      headers: { "X-CSRF-Token": localStorage.getItem("CSRF") },
      credentials: "include",
    })
      .then(response => response.json())
      .then(data => {
        if (data && data.sampleCategories) {
          setValidSampleCategories(data.sampleCategories.map(sc => sc.description || sc));
        } else {
          // Falls back to hardcoded values
          setValidSampleCategories([
            "Whole Plant", "Plant Part", "Plant Extract", "Fractionated Extract",
            "Purified Compound", "Formulated Product", "Reference Standard"
          ]);
        }
      })
      .catch(error => {
        // Falls back to hardcoded values on error
        setValidSampleCategories([...hardcoded values...]);
      });
  }
}, [open]);
```

**Current Workaround:** Frontend gracefully falls back to hardcoded values when
the endpoint fails, so functionality works but:

- Categories cannot be dynamically managed in the backend
- Lab admins cannot add/update sample categories without code changes
- The endpoint is silently failing (no error feedback to user)

**Recommendation:** Add a GET endpoint to fetch valid sample categories from the
database.

---

### Gap 2: Missing Advanced Features (Bioanalytical Reference)

**Impact Level:** MEDIUM 🟡

**Bioanalytical Advanced Features Not in TMMRD:**

1. Pagination with offset/limit validation
2. Filtering capability
3. Multiple validated lists (tests, source-origins)

**For TMMRD Context:**

- TMMRD is simpler than Bioanalytical (doesn't have "tests" or "source-origins"
  concepts)
- Pagination is less critical for TMMRD (sample categories list is typically
  small ~7-15 items)
- Recommendation: Implement sample-categories endpoint, defer pagination unless
  needed

---

## Implementation Recommendations

### Priority 1: Add Sample Categories Endpoint (IMPLEMENTED ✅)

**Endpoint:** `GET /rest/notebook/tradmed/sample-categories`

**Purpose:** Fetch valid sample categories for dropdown validation in manifest
import modal

**Status:** ✅ **COMPLETED** - See
[TMMRD-SAMPLE-CATEGORIES-ENDPOINT.md](TMMRD-SAMPLE-CATEGORIES-ENDPOINT.md) for
implementation details

**Changes Implemented:**

1. **Update Controller** (TraditionalMedicineManifestImportController.java)

Add this method:

```java
@GetMapping(value = "/sample-categories", produces = MediaType.APPLICATION_JSON_VALUE)
@ResponseBody
public ResponseEntity<Map<String, Object>> getValidSampleCategories() {
    List<Map<String, String>> categories = traditionalMedicineManifestImportService
        .getValidSampleCategories();

    Map<String, Object> response = new HashMap<>();
    response.put("sampleCategories", categories);
    response.put("total", categories.size());

    return ResponseEntity.ok(response);
}
```

2. **Update Service** (TraditionalMedicineManifestImportService.java)

Add this method:

```java
public List<Map<String, String>> getValidSampleCategories() {
    return Arrays.asList(
        Map.of("id", "whole_plant", "description", "Whole Plant"),
        Map.of("id", "plant_part", "description", "Plant Part"),
        Map.of("id", "plant_extract", "description", "Plant Extract"),
        Map.of("id", "fractionated_extract", "description", "Fractionated Extract"),
        Map.of("id", "purified_compound", "description", "Purified Compound"),
        Map.of("id", "formulated_product", "description", "Formulated Product"),
        Map.of("id", "reference_standard", "description", "Reference Standard")
    );
}
```

**Response Format:**

```json
{
  "sampleCategories": [
    { "id": "whole_plant", "description": "Whole Plant" },
    { "id": "plant_part", "description": "Plant Part" },
    { "id": "plant_extract", "description": "Plant Extract" },
    { "id": "fractionated_extract", "description": "Fractionated Extract" },
    { "id": "purified_compound", "description": "Purified Compound" },
    { "id": "formulated_product", "description": "Formulated Product" },
    { "id": "reference_standard", "description": "Reference Standard" }
  ],
  "total": 7
}
```

**Affected Files:**

- `src/main/java/org/openelisglobal/notebook/controller/rest/TraditionalMedicineManifestImportController.java`
  (add @GetMapping)
- `src/main/java/org/openelisglobal/notebook/service/TraditionalMedicineManifestImportService.java`
  (add method)

**Benefit:**

- Eliminates frontend hardcoding of categories
- Enables future dynamic category management
- Aligns TMMRD with Immunology/Bioanalytical patterns
- Frontend can remove fallback logic (optional cleanup)

---

### Priority 2: Consistency Improvements (Recommended)

**Status Quo Analysis:**

| Aspect                           | TMMRD                                       | Immunology                                  | Bioanalytical                          |
| -------------------------------- | ------------------------------------------- | ------------------------------------------- | -------------------------------------- |
| Sample Types/Categories Endpoint | ❌ Missing                                  | ✅ /sample-types                            | ✅ /sample-types                       |
| Endpoint Naming                  | `/preview-manifest` `/create-from-manifest` | `/preview-manifest` `/create-from-manifest` | `/preview-manifest` `/import-manifest` |
| Response Field (Created IDs)     | `createdAccessionNumbers`                   | `createdAccessionNumbers`                   | `createdSampleIds`                     |

**Recommendations:**

1. Add sample-categories GET endpoint (high priority)
2. Consider renaming `/create-from-manifest` to `/import-manifest` for
   consistency with Bioanalytical (optional, low priority)
3. Keep response field as `createdAccessionNumbers` - it's appropriate for TMMRD
   since samples get accession numbers on creation

---

## Frontend Manifest Modal Usage Pattern

**Current Flow:**

```
1. Modal Opens
   ↓
2. Frontend calls GET /rest/notebook/tradmed/sample-categories
   ↓
3a. If successful → Display fetched categories in dropdown
3b. If error/timeout → Fall back to hardcoded categories
   ↓
4. User uploads CSV file
   ↓
5. Frontend auto-maps columns and calls POST /preview-manifest
   ↓
6. User confirms mapping and calls POST /create-from-manifest
   ↓
7. Samples created in database
```

**Current Implementation in Modal:**

- Location:
  `frontend/src/components/notebook/workflow/TraditionalMedicineManifestImportModal.js`
- Lines 192-241: Sample category fetching logic with graceful fallback
- The endpoint is expected but not breaking when missing (good UX)

---

## Testing Checklist for GET /sample-categories Endpoint

```
[ ] Endpoint returns 200 OK with proper JSON structure
[ ] Response includes all 7 expected sample categories
[ ] Response includes id and description for each category
[ ] Response includes "total" count field
[ ] Frontend receives data and populates dropdown
[ ] Dropdown validation against returned categories works
[ ] CSV validation rejects invalid category values
[ ] Graceful fallback still works if endpoint is slow/timeout
[ ] Endpoint is accessible without authentication (review if needed)
[ ] CSRF token validation passes
```

---

## Related Files

**Backend:**

- Controller:
  `src/main/java/org/openelisglobal/notebook/controller/rest/TraditionalMedicineManifestImportController.java`
- Service:
  `src/main/java/org/openelisglobal/notebook/service/TraditionalMedicineManifestImportService.java`
- Form:
  `src/main/java/org/openelisglobal/notebook/form/TraditionalMedicineManifestImportForm.java`

**Frontend:**

- Modal:
  `frontend/src/components/notebook/workflow/TraditionalMedicineManifestImportModal.js`
  (lines 192-241)
- Page 1:
  `frontend/src/components/notebook/pages/traditionalmedicine/TraditionalMedicineSampleCreationPage.js`

**Reference Implementations:**

- Immunology:
  `src/main/java/org/openelisglobal/notebook/controller/rest/ImmunologyManifestImportController.java`
- Bioanalytical:
  `src/main/java/org/openelisglobal/notebook/controller/rest/BioanalyticalManifestImportController.java`

**Test Data:**

- Manifest: `test-data/tradmed-sample-manifest-example.csv`

---

## Summary

| Item                       | Status                   | Priority | Action                                                   |
| -------------------------- | ------------------------ | -------- | -------------------------------------------------------- |
| Preview manifest endpoint  | ✅ Works                 | -        | No change needed                                         |
| Create/import endpoint     | ✅ Works                 | -        | No change needed                                         |
| Sample categories endpoint | ❌ Missing               | HIGH     | **Implement GET /sample-categories**                     |
| Pagination support         | ❌ Missing               | LOW      | Defer until needed                                       |
| Filtering support          | ❌ Missing               | LOW      | Defer until needed                                       |
| Frontend integration       | ✅ Works (with fallback) | -        | No change needed (optional cleanup after endpoint added) |

**Next Steps:**

1. Implement GET `/rest/notebook/tradmed/sample-categories` endpoint
2. Test with manifest import modal
3. Verify CSV validation uses returned categories
4. (Optional) Update frontend to remove hardcoded fallback logic

---

**Last Updated:** January 2025 **Analysis Scope:** TMMRD vs Immunology,
Bioanalytical, Bioequivalence labs
