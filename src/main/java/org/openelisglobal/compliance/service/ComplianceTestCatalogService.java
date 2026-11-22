package org.openelisglobal.compliance.service;

import java.util.List;
import org.openelisglobal.compliance.controller.rest.TestCatalogEntry;
import org.openelisglobal.compliance.controller.rest.TestCatalogEntryWithCompliance;

/**
 * Read-only catalog assembly for the compliance UI. Augments the basic test
 * catalog with per-test sample types, result type, and (for dictionary-backed
 * tests) the predefined select options. Encapsulates the cross-service walk
 * that previously lived in {@code ComplianceTestCatalogRestController} so the
 * web layer stays a thin request mapper and the lazy-association traversal is
 * scoped to a service-level transaction.
 */
public interface ComplianceTestCatalogService {

    /** Catalog rows for every active test. */
    List<TestCatalogEntry> getCatalog();

    /**
     * Catalog rows for tests that have at least one compliance threshold linked,
     * each carrying {@code thresholdCount} and {@code standardCount}.
     */
    List<TestCatalogEntryWithCompliance> getCatalogWithCompliance();

    /**
     * Distinct, sorted sample-type category names used by the Standard form's
     * Applicable Sample Types dropdown.
     */
    List<String> getSampleTypeCategories();
}
