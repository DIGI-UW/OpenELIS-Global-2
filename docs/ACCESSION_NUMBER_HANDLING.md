# Generic Accession Number Handler - Implementation Guide

## Overview

The `AccessionNumberHandler` is a reusable utility class designed to safely generate and insert samples with unique accession numbers across all manifest import service implementations. It provides thread-safe mechanisms to handle duplicate accession number violations, both from concurrent requests within a single application instance and from race conditions in multi-instance deployments.

## Problem Statement

**Issue**: Duplicate accession number violations when importing samples via manifest files.

```
ERROR: duplicate key value violates unique constraint "accnum_uk"
Detail: Key (accession_number)=(2026000001) already exists.
```

**Root Causes**:
1. **Concurrent imports** - Multiple import requests happening simultaneously within the same application instance
2. **Multi-instance race condition** - One instance checks for duplicate, but another instance inserts the same number before the first completes
3. **Missing exception handling** - No retry logic when database constraint violations occur

## Solution Architecture

### Components

#### 1. DuplicateAccessionNumberException
**Location**: `org.openelisglobal.sample.exception.DuplicateAccessionNumberException`

Custom exception class thrown when duplicate accession numbers are detected. Includes:
- The conflicting accession number
- Attempt number and maximum attempts (for diagnostics)
- Underlying exception cause

```java
public DuplicateAccessionNumberException(String accessionNumber, int attemptNumber,
        int maxAttempts, Throwable cause)
```

#### 2. AccessionNumberHandler
**Location**: `org.openelisglobal.sample.util.AccessionNumberHandler`

Generic utility class that provides:
- **Synchronized access** - Uses static lock to prevent concurrent duplicate generation within a single JVM
- **Database verification** - Checks if generated accession number already exists before insertion
- **Retry logic** - Automatically retries up to 100 times if duplicates are detected
- **Exception handling** - Catches `DataIntegrityViolationException` and retries for race conditions
- **Database flush** - Immediately flushes changes to ensure visibility across concurrent operations

### How It Works

```
┌─────────────────────────────────────────────────┐
│ Generate accession number candidate             │
└─────────────────────────────────────┬───────────┘
                                      │
                    ┌─────────────────▼──────────┐
                    │ Check if exists in DB      │
                    └─────────────────┬──────────┘
                                      │
              ┌───────────────────────┴──────────────────┐
              │                                          │
         YES  │                                      NO  │
              │                                          │
    ┌─────────▼──────────┐                  ┌───────────▼──────────┐
    │ Log warning        │                  │ Set accession number │
    │ Retry (attempt+1)  │                  │ Insert into DB       │
    └─────────┬──────────┘                  │ Flush                │
              │                             └───────────┬──────────┘
              │                                         │
              │                                     SUCCESS
              │                                         │
              │                                 ┌───────▼──────────┐
              │                                 │ Return Sample ID │
              │                                 └──────────────────┘
              │
         Max attempts
           exceeded?
              │
              YES (throw exception)
```

## Usage Guide

### Basic Usage - Single Row Import

```java
@Service
public class YourManifestImportService {

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleDAO sampleDAO;

    @PersistenceContext
    private EntityManager entityManager;

    public String createSampleWithUniqueAccessionNumber(String userId) {
        // Create sample with required fields
        Sample sample = new Sample();
        sample.setSysUserId(userId);
        sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
        sample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));

        // Use handler to safely generate and insert
        try {
            AccessionNumberHandler handler = new AccessionNumberHandler(
                sampleService, sampleDAO, entityManager, this.getClass()
            );
            String sampleId = handler.generateAndInsertWithUniqueAccessionNumber(sample);
            return sampleId;
        } catch (DuplicateAccessionNumberException e) {
            // Handle failure after all retries exhausted
            throw new RuntimeException("Failed to create sample: " + e.getMessage(), e);
        }
    }
}
```

### Advanced Usage - Batch Import with Error Collection

```java
@Transactional
public VirologyManifestImportResult createSamplesForEntry(
        Integer entryId, ParsedManifest manifest, String userId) {

    List<String> createdAccessionNumbers = new ArrayList<>();
    List<ParseError> errors = new ArrayList<>();

    AccessionNumberHandler handler = new AccessionNumberHandler(
        sampleService, sampleDAO, entityManager, this.getClass()
    );

    for (VirologyManifestRow row : manifest.rows()) {
        Sample parentSample = new Sample();
        parentSample.setSysUserId(userId);
        parentSample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
        parentSample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));

        try {
            String sampleId = handler.generateAndInsertWithUniqueAccessionNumber(
                parentSample
            );
            parentSample.setId(sampleId);

            // Continue with sample item creation, linking to notebook, etc.
            createdAccessionNumbers.add(parentSample.getAccessionNumber());

        } catch (DuplicateAccessionNumberException e) {
            // Log error and continue with next row
            errors.add(new ParseError(row.rowNumber(), "sample",
                "Failed to generate unique accession number: " + e.getMessage()));
            LogEvent.logError("Duplicate accession number error for row " + row.rowNumber(), e);
            continue;
        }
    }

    return new VirologyManifestImportResult(
        createdAccessionNumbers, errors, createdAccessionNumbers.size()
    );
}
```

### Custom Retry Count

```java
// Use custom maximum attempts (default is 100)
String sampleId = handler.generateAndInsertWithUniqueAccessionNumber(
    sample,
    50  // Only retry 50 times instead of default 100
);
```

## Integration Checklist

To integrate `AccessionNumberHandler` into a manifest import service:

- [ ] Add import statements:
  ```java
  import org.openelisglobal.sample.exception.DuplicateAccessionNumberException;
  import org.openelisglobal.sample.util.AccessionNumberHandler;
  ```

- [ ] Remove any existing synchronized blocks or static lock objects
- [ ] Remove direct calls to `sampleDAO.getNextAccessionNumber()` in sample creation logic
- [ ] Remove inline retry logic for accession number generation
- [ ] Replace sample creation code with AccessionNumberHandler call
- [ ] Add try-catch block to handle `DuplicateAccessionNumberException`
- [ ] Add error collection for failed samples
- [ ] Ensure EntityManager is autowired: `@PersistenceContext private EntityManager entityManager;`

## Implementation Examples

### VirologyManifestImportServiceImpl (Already Updated)

**Location**: `src/main/java/org/openelisglobal/notebook/service/VirologyManifestImportServiceImpl.java`

**Changes Made**:
- Replaced synchronized block with `AccessionNumberHandler`
- Updated exception handling to catch `DuplicateAccessionNumberException`
- Added detailed error logging
- Removed `getNextAccessionNumberInternal()` method
- Removed static `ACCESSION_NUMBER_LOCK` field

**Result**:
- Handles duplicate accession numbers gracefully
- Automatically retries up to 100 times
- Provides clear error messages to users
- Maintains transaction safety

### Services Ready for Update

These services use similar patterns and can be updated using the same approach:

1. **BacteriologyManifestImportServiceImpl**
   - Location: `src/main/java/org/openelisglobal/notebook/service/BacteriologyManifestImportServiceImpl.java`

2. **BioanalyticalManifestImportServiceImpl**
   - Location: `src/main/java/org/openelisglobal/notebook/service/BioanalyticalManifestImportServiceImpl.java`

3. **TBManifestImportServiceImpl**
   - Location: `src/main/java/org/openelisglobal/notebook/service/TBManifestImportServiceImpl.java`

4. **PharmaManifestImportServiceImpl**
   - Location: `src/main/java/org/openelisglobal/notebook/service/PharmaManifestImportServiceImpl.java`

5. **ImmunologyManifestImportServiceImpl**
6. **PathologyManifestImportServiceImpl**
7. **MNTDManifestImportServiceImpl**
8. **GBDManifestImportServiceImpl**
9. **TraditionalMedicineManifestImportServiceImpl**

### Generic ManifestImportServiceImpl

The base `ManifestImportServiceImpl` should also be updated as it's the foundation for many lab-specific implementations:

- Location: `src/main/java/org/openelisglobal/notebook/service/ManifestImportServiceImpl.java`

## Exception Handling Strategy

### Catching DuplicateAccessionNumberException

```java
try {
    String sampleId = handler.generateAndInsertWithUniqueAccessionNumber(sample);
} catch (DuplicateAccessionNumberException e) {
    // Option 1: Log and continue (for batch imports)
    errors.add(new ParseError(rowNumber, "sample",
        "Failed to generate accession number: " + e.getMessage()));
    continue;

    // Option 2: Re-throw with context
    throw new ManifestImportException("Row " + rowNumber + ": " + e.getMessage(), e);

    // Option 3: Use default accession number (not recommended)
    sample.setAccessionNumber(generateFallbackAccessionNumber());
}
```

### Detecting Duplicate Key Violations

The handler automatically detects duplicate accession number violations by checking for:
- PostgreSQL unique constraint name `accnum_uk`
- Column name `accession_number` in error message
- Underlying `PSQLException` with `accnum_uk` reference

## Performance Considerations

### Thread Safety
- **Single Instance**: Synchronized block prevents concurrent duplicates within one JVM
- **Multiple Instances**: Exception handling catches race conditions between instances
- **Database Visibility**: `entityManager.flush()` ensures changes are immediately visible

### Retry Behavior
- **Default Max Attempts**: 100 retries
- **Typical Success**: Usually succeeds on 1st or 2nd attempt
- **Rare Cases**: Retries only needed for true race conditions between multiple instances

### Database Load
- **Per Sample**: 1-2 SELECT queries (checking for duplicates)
- **Batch Import**: Linear with sample count
- **No N+1 Problem**: Each sample check is independent

## Troubleshooting

### Still Getting Duplicate Key Errors?

**Symptoms**:
```
ERROR: duplicate key value violates unique constraint "accnum_uk"
```

**Causes and Solutions**:

1. **Multiple Application Instances Running**
   - **Solution**: Add database-level uniqueness constraint with sequence:
     ```sql
     CREATE SEQUENCE accession_number_seq INCREMENT BY 1 NOCACHE NOORDER;
     ALTER TABLE clinlims.sample ADD CONSTRAINT accession_number_unique
       UNIQUE (accession_number);
     ```

2. **Handler Not Being Used in All Sample Creation Paths**
   - **Solution**: Audit all `sampleService.insertDataWithAccessionNumber()` calls
   - Ensure none bypass the handler

3. **Concurrent Imports to Different Controllers**
   - **Solution**: Verify all manifest import controllers use AccessionNumberHandler
   - Check MedLabManifestImportController, BacteriologyManifestImportController, etc.

4. **Old Code Still in Production**
   - **Solution**: Ensure services are redeployed after code changes
   - Clear application cache if applicable

### Performance Issues with Batch Imports?

**Symptoms**: Slow manifest import for large files (500+ rows)

**Solutions**:

1. **Reduce retry count** if your duplicates are rare:
   ```java
   handler.generateAndInsertWithUniqueAccessionNumber(sample, 50);
   ```

2. **Pre-generate accession numbers** in bulk:
   ```java
   List<String> preGeneratedNumbers = sampleDAO.generateBulkAccessionNumbers(100);
   for (String accNum : preGeneratedNumbers) {
       // Use pre-generated numbers
   }
   ```

3. **Use database-level accession number sequences** instead of application generation

## Testing

### Unit Test Example

```java
@Test
public void testDuplicateAccessionNumberRetry() {
    Sample sample = new Sample();
    sample.setSysUserId("1");
    sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
    sample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));

    // Mock: First call returns existing sample (duplicate), second call returns null
    when(sampleService.getSampleByAccessionNumber(any()))
        .thenReturn(new Sample())  // First attempt: exists
        .thenReturn(null);          // Second attempt: doesn't exist

    AccessionNumberHandler handler = new AccessionNumberHandler(
        sampleService, sampleDAO, entityManager, this.getClass()
    );

    String sampleId = handler.generateAndInsertWithUniqueAccessionNumber(sample);

    // Verify retry happened
    verify(sampleService, times(2)).getSampleByAccessionNumber(any());
    assertNotNull(sampleId);
}
```

### Integration Test Example

```java
@SpringBootTest
public class AccessionNumberHandlerIntegrationTest {

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleDAO sampleDAO;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    public void testConcurrentImportsWithoutDuplicates() {
        AccessionNumberHandler handler1 = new AccessionNumberHandler(
            sampleService, sampleDAO, entityManager, this.getClass()
        );
        AccessionNumberHandler handler2 = new AccessionNumberHandler(
            sampleService, sampleDAO, entityManager, this.getClass()
        );

        // Simulate concurrent sample creation
        Future<String> future1 = executorService.submit(() -> {
            Sample s1 = createSample("user1");
            return handler1.generateAndInsertWithUniqueAccessionNumber(s1);
        });

        Future<String> future2 = executorService.submit(() -> {
            Sample s2 = createSample("user2");
            return handler2.generateAndInsertWithUniqueAccessionNumber(s2);
        });

        String id1 = future1.get();
        String id2 = future2.get();

        assertNotEquals(id1, id2);
        // Verify both samples were created successfully
        assertNotNull(sampleService.get(id1));
        assertNotNull(sampleService.get(id2));
    }
}
```

## FAQ

**Q: Why do I need to pass EntityManager to the handler?**
A: The handler calls `entityManager.flush()` after each successful insertion to ensure database changes are immediately visible to other threads/instances, reducing race conditions.

**Q: Can I use this handler outside of @Transactional methods?**
A: Yes, but the handler assumes an active EntityManager is available. For non-transactional contexts, ensure you have an active transaction or session.

**Q: What if accession number generation fails completely?**
A: If the generator is unavailable, the handler falls back to `sampleDAO.getNextAccessionNumber()`. If that also fails, a `DuplicateAccessionNumberException` is thrown.

**Q: How do I update other manifest import services?**
A: Follow the "Integration Checklist" section and use VirologyManifestImportServiceImpl as a reference implementation.

**Q: Can this handler be used outside manifest imports?**
A: Yes! Any service that needs to safely generate and insert samples with unique accession numbers can use this handler. It's completely service-agnostic.

## Migration Path

### Phase 1: Core Services (Week 1)
- ✅ VirologyManifestImportServiceImpl (completed)
- [ ] ManifestImportServiceImpl (base)
- [ ] BacteriologyManifestImportServiceImpl

### Phase 2: Laboratory-Specific Services (Week 2)
- [ ] BioanalyticalManifestImportServiceImpl
- [ ] TBManifestImportServiceImpl
- [ ] PharmaManifestImportServiceImpl

### Phase 3: Remaining Services (Week 3)
- [ ] ImmunologyManifestImportServiceImpl
- [ ] PathologyManifestImportServiceImpl
- [ ] MNTDManifestImportServiceImpl
- [ ] GBDManifestImportServiceImpl
- [ ] TraditionalMedicineManifestImportServiceImpl

### Phase 4: Non-Manifest Sample Creation (Week 4)
- [ ] Review other services that create samples directly
- [ ] Update any that bypass manifest imports

## Related Classes

- `Sample` - `org.openelisglobal.sample.valueholder.Sample`
- `SampleService` - `org.openelisglobal.sample.service.SampleService`
- `SampleDAO` - `org.openelisglobal.sample.dao.SampleDAO`
- `AccessionNumberUtil` - `org.openelisglobal.sample.util.AccessionNumberUtil`
- `IAccessionNumberGenerator` - `org.openelisglobal.common.provider.validation.IAccessionNumberGenerator`

## Support

For issues or questions:
1. Check the Troubleshooting section above
2. Review VirologyManifestImportServiceImpl as reference
3. Check test files for integration examples
4. Contact the development team
