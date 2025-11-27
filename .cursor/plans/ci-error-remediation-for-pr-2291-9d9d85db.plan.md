<!-- 9d9d85db-8d5d-49c7-90b0-38b63b072bde 876adf01-c390-4838-abc5-cd5ffacaf0b2 -->

# Fix BarcodeLabelMaker Static Initializer with Lazy Initialization

## Problem Analysis

The `BarcodeLabelMaker` class has a static initializer that runs when the class
is first loaded, attempting to access
`SpringContext.getBean(IStatusService.class)` before Spring is initialized. This
causes `NumberFormatException: Cannot parse null string` in tests because:

1. The static initializer runs at class load time (when
   `new BarcodeLabelMaker()` is first called)
2. At that point, `StatusService.getStatusID(SampleStatus.Entered)` may return
   `null` if StatusService maps aren't built yet
3. `Integer.parseInt(null)` throws `NumberFormatException`
4. This prevents the class from loading, causing `NoClassDefFoundError` in
   subsequent uses

## Solution: Lazy Initialization Pattern

Replace the static initializer with a thread-safe lazy initialization method
that:

- Initializes the set on first use (when Spring is guaranteed to be ready)
- Uses double-checked locking for thread safety
- Handles null, "-1", and NumberFormatException gracefully
- Logs errors but doesn't fail catastrophically

## Implementation Plan

### File: `src/main/java/org/openelisglobal/barcode/BarcodeLabelMaker.java`

**Changes:**

1.  **Remove static initializer** (lines 102-105):

                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                - Delete the static block that calls `SpringContext.getBean()` and `Integer.parseInt()`

2.  **Add lazy initialization method**:

                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                - Create `private static synchronized Set<Integer> getEnteredStatusSampleList()` method
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                - Use double-checked locking pattern
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                - Handle null, "-1", and exceptions gracefully
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                - Log errors using `LogEvent.logError()`

3.  **Update usage sites** (lines 211 and 242):

                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                - Replace direct `ENTERED_STATUS_SAMPLE_LIST` usage with `getEnteredStatusSampleList()`
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                - This ensures initialization happens before use

**Code Pattern:**

```java
private static final Set<Integer> ENTERED_STATUS_SAMPLE_LIST = new HashSet<>();
private static volatile boolean initialized = false;

/**
 * Lazy initialization of ENTERED_STATUS_SAMPLE_LIST.
 * Initializes on first use to ensure SpringContext is ready.
 * Thread-safe using double-checked locking pattern.
 *
 * @return Set containing the status ID for SampleStatus.Entered, or empty set if initialization fails
 */
private static Set<Integer> getEnteredStatusSampleList() {
    if (!initialized) {
        synchronized (ENTERED_STATUS_SAMPLE_LIST) {
            if (!initialized) {
                try {
                    IStatusService statusService = SpringContext.getBean(IStatusService.class);
                    String statusId = statusService.getStatusID(SampleStatus.Entered);

                    if (statusId != null && !statusId.equals("-1") && !statusId.trim().isEmpty()) {
                        ENTERED_STATUS_SAMPLE_LIST.add(Integer.parseInt(statusId));
                        initialized = true;
                    } else {
                        LogEvent.logError("BarcodeLabelMaker", "getEnteredStatusSampleList",
                                "SampleStatus.Entered not found in database. Status ID: " + statusId);
                    }
                } catch (NumberFormatException e) {
                    LogEvent.logError("BarcodeLabelMaker", "getEnteredStatusSampleList",
                            "Failed to parse status ID: " + e.getMessage());
                } catch (Exception e) {
                    LogEvent.logError("BarcodeLabelMaker", "getEnteredStatusSampleList",
                            "Failed to initialize ENTERED_STATUS_SAMPLE_LIST: " + e.getMessage());
                }
            }
        }
    }
    return ENTERED_STATUS_SAMPLE_LIST;
}
```

**Usage Updates:**

Line 211: Change from:

```java
List<SampleItem> sampleItemList = sampleItemService.getSampleItemsBySampleIdAndStatus(sample.getId(),
        ENTERED_STATUS_SAMPLE_LIST);
```

To:

```java
List<SampleItem> sampleItemList = sampleItemService.getSampleItemsBySampleIdAndStatus(sample.getId(),
        getEnteredStatusSampleList());
```

Line 242: Same change

### Testing Strategy

1. **Verify existing functionality**: Run tests that use BarcodeLabelMaker to
   ensure they still pass
2. **Verify test fix**: Run `LabelManagementRestControllerTest` to confirm the 3
   failing tests now pass
3. **Verify thread safety**: Ensure no race conditions in multi-threaded
   scenarios
4. **Verify error handling**: Test behavior when StatusService returns null or
   "-1"

### Files to Modify

- `src/main/java/org/openelisglobal/barcode/BarcodeLabelMaker.java`: - Remove
  static initializer (lines 102-105) - Add `getEnteredStatusSampleList()`
  method - Update usage at lines 211 and 242

### Expected Outcomes

- All 3 failing tests in `LabelManagementRestControllerTest` pass
- No regression in existing BarcodeLabelMaker functionality
- Thread-safe initialization
- Graceful error handling if status not found

### Risk Assessment

**Low Risk:**

- Changes are isolated to BarcodeLabelMaker class
- Lazy initialization is a well-established pattern
- Error handling prevents catastrophic failures
- Thread-safe implementation prevents race conditions

**Verification:**

- Run full test suite to ensure no regressions
- Specifically verify `LabelManagementRestControllerTest` passes
- Verify `BarcodeLableInforServiceTest` still passes (uses BarcodeLabelMaker
  indirectly)
