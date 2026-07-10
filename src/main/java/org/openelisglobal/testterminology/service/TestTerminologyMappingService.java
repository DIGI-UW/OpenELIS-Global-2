package org.openelisglobal.testterminology.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.testterminology.valueholder.TestTerminologyMapping;

@CrossDomainService(callers = "Test catalog editor (TestAddService/TestModifyService, PRIV_TEST_CONFIGURE context)"
        + " and the FHIR transform pipeline (FhirTransformServiceImpl LOINC resolution during export), which runs"
        + " outside an authenticated user context")
public interface TestTerminologyMappingService extends BaseObjectService<TestTerminologyMapping, String> {

    /** Active terminology mappings for a test. */
    List<TestTerminologyMapping> getActiveByTestId(String testId);

    /**
     * OGC-949 M10: reconcile a test's terminology mappings to exactly the desired
     * set, in one transaction. Identity is the natural key {@code (source, code)}
     * (which the DB also enforces unique per test): a desired mapping whose
     * {@code (source, code)} already exists is updated/reactivated rather than
     * re-inserted — so re-adding a previously-removed code never collides with the
     * unique constraint. Existing active mappings absent from {@code desired} are
     * soft-deleted ({@code is_active = 'N'}).
     */
    void saveMappingsForTest(String testId, List<TestTerminologyMapping> desired, String sysUserId);

    /**
     * Reconcile the single legacy {@code test.loinc} value into the terminology
     * mappings so the new editor reflects edits made on the legacy Test Modify
     * page. A non-blank {@code loinc} upserts the matching LOINC mapping (new rows
     * default to {@code SAME_AS}) and soft-deletes any other active LOINC mapping
     * with a different code; a blank {@code loinc} soft-deletes all active LOINC
     * mappings. Non-LOINC mappings (SNOMED/CIEL/OCL) are left untouched.
     */
    void syncLegacyLoinc(String testId, String loinc, String sysUserId);

    /**
     * OGC-1145 (FR-14) — active mappings for a standard-terminology code, honoring
     * specimen scope: mappings scoped to {@code sampleTypeId} win over shared
     * ({@code sample_type_id} null) mappings; the shared set is returned when no
     * specimen-scoped mapping exists (or no specimen is given).
     */
    List<TestTerminologyMapping> getActiveMappingsForCode(String source, String code, String sampleTypeId);

    /**
     * Test ids carrying at least one active mapping for {@code source}, in any
     * scope — whole test, a component, or one specimen. One query, so a catalog
     * listing can decorate every row without a per-test lookup.
     */
    java.util.Set<String> getTestIdsWithActiveSource(String source);

    /**
     * Whether this test has any active mapping for {@code source}, in any scope.
     */
    boolean hasActiveMappingForSource(String testId, String source);
}
