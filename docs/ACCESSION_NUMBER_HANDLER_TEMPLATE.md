# AccessionNumberHandler - Implementation Template

This template shows the exact changes needed to integrate
`AccessionNumberHandler` into any manifest import service.

## Step 1: Add Imports

Add these imports to your service class:

```java
import org.openelisglobal.sample.exception.DuplicateAccessionNumberException;
import org.openelisglobal.sample.util.AccessionNumberHandler;
```

## Step 2: Ensure Required Autowired Fields Exist

Your service must have these autowired dependencies:

```java
@Autowired
private SampleService sampleService;

@Autowired
private SampleDAO sampleDAO;

@PersistenceContext
private EntityManager entityManager;
```

## Step 3: Find the Sample Creation Code

Look for code that looks like this in your `createSamplesForEntry()` or similar
method:

### OLD CODE (Before)

```java
// ❌ OLD - Inline accession number generation
Sample parentSample = new Sample();
parentSample.setSysUserId(sysUserId);
parentSample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
parentSample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));

// Old approach: direct call with no retry
String sampleIdDb = sampleService.generateAccessionNumberAndInsert(parentSample);
parentSample.setId(sampleIdDb);

// Or: manual synchronized block (if it exists)
synchronized (SOME_LOCK) {
    // Generate accession number
    // Check for duplicates
    // Insert sample
}
```

### NEW CODE (After)

```java
// ✅ NEW - Using AccessionNumberHandler
Sample parentSample = new Sample();
parentSample.setSysUserId(sysUserId);
parentSample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
parentSample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));

// Create handler
AccessionNumberHandler handler = new AccessionNumberHandler(
    sampleService, sampleDAO, entityManager, this.getClass()
);

// Use handler with exception handling
String sampleIdDb;
try {
    sampleIdDb = handler.generateAndInsertWithUniqueAccessionNumber(parentSample);
    parentSample.setId(sampleIdDb);
} catch (DuplicateAccessionNumberException e) {
    // Handle error - log and continue with next row
    errors.add(new ParseError(row.rowNumber(), "sample",
        "Failed to generate unique accession number: " + e.getMessage()));
    LogEvent.logError("Duplicate accession number error for row " + row.rowNumber(), e);
    continue;
}
```

## Step 4: Remove Unused Code

Delete these if they exist in your service:

### Remove Static Lock Object

```java
// ❌ DELETE THIS
private static final Object ACCESSION_NUMBER_LOCK = new Object();
```

### Remove Accession Number Generator Field

```java
// ❌ DELETE THIS
private IAccessionNumberGenerator accessionNumberGenerator;
```

### Remove getNextAccessionNumberInternal() Method

```java
// ❌ DELETE THIS METHOD
private String getNextAccessionNumberInternal() {
    if (accessionNumberGenerator == null) {
        accessionNumberGenerator = AccessionNumberUtil.getMainAccessionNumberGenerator();
    }
    if (accessionNumberGenerator != null) {
        return accessionNumberGenerator.getNextAccessionNumber(null, true);
    }
    return sampleDAO.getNextAccessionNumber();
}
```

### Remove Unused Imports

```java
// ❌ DELETE THESE IF NOT USED ELSEWHERE
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.sample.util.AccessionNumberUtil;
```

## Complete Before & After Example

### BEFORE - ManifestImportServiceImpl Pattern

```java
public class SomeManifestImportServiceImpl implements SomeManifestImportService {

    private static final Object ACCESSION_NUMBER_LOCK = new Object();
    private IAccessionNumberGenerator accessionNumberGenerator;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleDAO sampleDAO;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public SomeManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId) {
        List<String> createdAccessionNumbers = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();
        int totalCreated = 0;

        for (SomeManifestRow row : manifest.rows()) {
            Sample parentSample = new Sample();
            parentSample.setSysUserId(sysUserId);
            parentSample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
            parentSample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));

            // ❌ OLD: Manual synchronized block with retry logic
            String sampleIdDb;
            synchronized (ACCESSION_NUMBER_LOCK) {
                String generatedAccessionNumber = null;
                int maxAttempts = 100;
                int attempts = 0;

                while (generatedAccessionNumber == null && attempts < maxAttempts) {
                    String candidateNumber = getNextAccessionNumberInternal();
                    Sample existingSample = sampleService.getSampleByAccessionNumber(candidateNumber);
                    if (existingSample == null) {
                        generatedAccessionNumber = candidateNumber;
                    } else {
                        attempts++;
                        LogEvent.logWarn(this.getClass().getSimpleName(), "createSamplesForEntry",
                                "Accession number " + candidateNumber + " already exists. Retrying");
                    }
                }

                if (generatedAccessionNumber == null) {
                    errors.add(new ParseError(row.rowNumber(), "sample",
                            "Failed to generate unique accession number after " + maxAttempts + " attempts"));
                    continue;
                }

                parentSample.setAccessionNumber(generatedAccessionNumber);
                sampleService.insertDataWithAccessionNumber(parentSample);
                sampleIdDb = parentSample.getId();
                entityManager.flush();
            }
            parentSample.setId(sampleIdDb);

            // Continue with sample item creation...
            createdAccessionNumbers.add(parentSample.getAccessionNumber());
            totalCreated++;
        }

        return new SomeManifestImportResult(createdAccessionNumbers, errors, totalCreated);
    }

    private String getNextAccessionNumberInternal() {
        if (accessionNumberGenerator == null) {
            accessionNumberGenerator = AccessionNumberUtil.getMainAccessionNumberGenerator();
        }
        if (accessionNumberGenerator != null) {
            return accessionNumberGenerator.getNextAccessionNumber(null, true);
        }
        return sampleDAO.getNextAccessionNumber();
    }
}
```

### AFTER - Using AccessionNumberHandler

```java
public class SomeManifestImportServiceImpl implements SomeManifestImportService {

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleDAO sampleDAO;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public SomeManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId) {
        List<String> createdAccessionNumbers = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();
        int totalCreated = 0;

        // ✅ NEW: Create handler once for all samples
        AccessionNumberHandler handler = new AccessionNumberHandler(
            sampleService, sampleDAO, entityManager, this.getClass()
        );

        for (SomeManifestRow row : manifest.rows()) {
            Sample parentSample = new Sample();
            parentSample.setSysUserId(sysUserId);
            parentSample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
            parentSample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));

            // ✅ NEW: Use handler with simple try-catch
            String sampleIdDb;
            try {
                sampleIdDb = handler.generateAndInsertWithUniqueAccessionNumber(parentSample);
                parentSample.setId(sampleIdDb);
            } catch (DuplicateAccessionNumberException e) {
                errors.add(new ParseError(row.rowNumber(), "sample",
                        "Failed to generate unique accession number: " + e.getMessage()));
                LogEvent.logError("Duplicate accession number error for row " + row.rowNumber(), e);
                continue;
            }

            // Continue with sample item creation...
            createdAccessionNumbers.add(parentSample.getAccessionNumber());
            totalCreated++;
        }

        return new SomeManifestImportResult(createdAccessionNumbers, errors, totalCreated);
    }
}
```

## Differences Summary

| Aspect             | Before                       | After                                   |
| ------------------ | ---------------------------- | --------------------------------------- |
| Lock Object        | Static field (1 per service) | Shared static in AccessionNumberHandler |
| Generator Field    | Private field in service     | Encapsulated in handler                 |
| Retry Logic        | 50-100 lines inline          | 1 line handler call                     |
| Exception Handling | Manual while loop            | Built-in with try-catch                 |
| Database Flush     | Manual entityManager.flush() | Automatic in handler                    |
| Reusability        | Service-specific             | Generic across all services             |
| Lines of Code      | ~40 lines                    | ~15 lines                               |
| Maintainability    | High (duplicated code)       | Low (single source of truth)            |

## Testing Your Changes

### 1. Compile Check

```bash
mvn clean compile -DskipTests -Dmaven.test.skip=true
```

### 2. Unit Test

Create a test that verifies the handler is called:

```java
@Test
public void testSampleCreationWithHandler() {
    // Mock setup
    Sample mockSample = new Sample();
    mockSample.setId("123");
    mockSample.setAccessionNumber("2026000001");

    when(sampleService.getSampleByAccessionNumber(any())).thenReturn(null);
    when(sampleDAO.getNextAccessionNumber()).thenReturn("2026000001");

    // Create sample
    String result = serviceUnderTest.createSampleWithUniqueAccession(userId);

    // Verify
    assertEquals("123", result);
    verify(sampleService).insertDataWithAccessionNumber(any());
}
```

### 3. Integration Test

Import and run against database:

```bash
mvn test -Dtest=SomeManifestImportServiceTest
```

### 4. Manual Testing

1. Upload manifest CSV file
2. Verify samples are created with unique accession numbers
3. Attempt concurrent uploads (stress test)
4. Check logs for "Duplicate accession number" messages (should see retries, not
   errors)

## Checklist for Migration

- [ ] Added imports for `DuplicateAccessionNumberException` and
      `AccessionNumberHandler`
- [ ] Sample creation code updated to use handler
- [ ] Exception handling added with try-catch
- [ ] Error collection updated with appropriate error messages
- [ ] Removed static `ACCESSION_NUMBER_LOCK` field
- [ ] Removed `accessionNumberGenerator` field
- [ ] Removed `getNextAccessionNumberInternal()` method
- [ ] Removed unused imports
- [ ] Code compiles without errors
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed
- [ ] Code review completed

## Common Issues & Solutions

### Issue: "Cannot find symbol: AccessionNumberHandler"

**Solution**: Make sure the import is correct:

```java
import org.openelisglobal.sample.util.AccessionNumberHandler;
```

### Issue: "EntityManager is not set"

**Solution**: Ensure `@PersistenceContext` annotation is present:

```java
@PersistenceContext
private EntityManager entityManager;
```

### Issue: "Handler not found in same transaction"

**Solution**: Create handler inside the `@Transactional` method, not as a
service-level dependency.

### Issue: Still getting duplicate key errors

**Solution**: Make sure you're using
`handler.generateAndInsertWithUniqueAccessionNumber()`, not the old method.

## Next Service to Update

After updating one service, use this template for:

1. **ManifestImportServiceImpl** (foundational service)
2. **BacteriologyManifestImportServiceImpl**
3. **BioanalyticalManifestImportServiceImpl**
4. **TBManifestImportServiceImpl**
5. **PharmaManifestImportServiceImpl**
6. Other lab-specific imports...

Each update takes approximately 10-15 minutes following this template.
