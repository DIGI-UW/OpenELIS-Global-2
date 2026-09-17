package org.openelisglobal.testsamplehandling.service;

import java.util.List;
import org.openelisglobal.configuration.service.AbstractCatalogCsvHandler;
import org.openelisglobal.configuration.service.CatalogReferenceResolver;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testsamplehandling.valueholder.TestSampleHandling;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Loads the {@code sample-handling} catalog domain from CSV: the storage and
 * disposal rules the Test Catalog editor's Sample Storage section keeps for a
 * test, one row per test.
 * <p>
 * Columns:
 * {@code testName,sampleType,storageCondition,storageConditionCustom,storageDuration,storageDurationUnit,stabilityNotes,protectFromLight,doNotFreeze,doNotRefrigerate,disposalMethod,disposalTimeframe,disposalUnit,specialInstructions}.
 * {@code testName} is required; {@code sampleType} only picks a legacy
 * {@code Name(Specimen)} record. A blank cell leaves the stored value as it is.
 * Written through {@link TestSampleHandlingService#saveForTest}.
 */
@Component
public class SampleHandlingConfigurationHandler extends AbstractCatalogCsvHandler {

    @Autowired
    private TestSampleHandlingService handlingService;

    @Autowired
    private CatalogReferenceResolver resolver;

    @Override
    public String getDomainName() {
        return "sample-handling";
    }

    @Override
    public int getLoadOrder() {
        return 320;
    }

    @Override
    protected String[] requiredColumns() {
        return new String[] { "testName" };
    }

    @Override
    protected void load(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary, String fileName) {
        for (CsvRow row : rows) {
            LoadedRow<String> outcome;
            try {
                outcome = transaction.run(() -> loadRow(row, fileName));
            } catch (Exception e) {
                outcome = LoadedRow.skipped(CsvLoadSummary.reason(e));
            }
            summary.record(outcome, getClass().getSimpleName(), row.lineNumber());
            flushUnresolvedReferences(fileName, row.lineNumber());
        }
    }

    private LoadedRow<String> loadRow(CsvRow row, String fileName) {
        String testName = row.get("testName");
        if (testName.isEmpty()) {
            return LoadedRow.skipped("missing testName");
        }
        Test test = resolver.resolveTest(testName, row.get("sampleType"),
                fileName + " line " + row.lineNumber() + " (sample handling)");
        if (test == null) {
            return LoadedRow.skipped("test '" + testName + "' not found");
        }
        TestSampleHandling existing = handlingService.getByTestId(test.getId());
        TestSampleHandling desired = existing == null ? new TestSampleHandling() : existing;
        desired.setTestId(test.getId());
        if (!row.isBlank("storageCondition")) {
            desired.setStorageCondition(row.get("storageCondition"));
        }
        if (!row.isBlank("storageConditionCustom")) {
            desired.setStorageConditionCustom(row.get("storageConditionCustom"));
        }
        if (!row.isBlank("storageDuration")) {
            desired.setStorageDuration(row.integer("storageDuration"));
        }
        if (!row.isBlank("storageDurationUnit")) {
            desired.setStorageDurationUnit(row.get("storageDurationUnit"));
        }
        if (!row.isBlank("stabilityNotes")) {
            desired.setStabilityNotes(row.get("stabilityNotes"));
        }
        if (!row.isBlank("protectFromLight")) {
            desired.setProtectFromLight(row.flag("protectFromLight", false));
        }
        if (!row.isBlank("doNotFreeze")) {
            desired.setDoNotFreeze(row.flag("doNotFreeze", false));
        }
        if (!row.isBlank("doNotRefrigerate")) {
            desired.setDoNotRefrigerate(row.flag("doNotRefrigerate", false));
        }
        if (!row.isBlank("disposalMethod")) {
            desired.setDisposalMethod(row.get("disposalMethod"));
        }
        if (!row.isBlank("disposalTimeframe")) {
            desired.setDisposalTimeframe(row.integer("disposalTimeframe"));
        }
        if (!row.isBlank("disposalUnit")) {
            desired.setDisposalUnit(row.get("disposalUnit"));
        }
        if (!row.isBlank("specialInstructions")) {
            desired.setSpecialInstructions(row.get("specialInstructions"));
        }
        handlingService.saveForTest(test.getId(), desired, SYS_USER_ID);
        return existing == null ? LoadedRow.created(test.getId()) : LoadedRow.updated(test.getId());
    }
}
