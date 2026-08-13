package org.openelisglobal.sampletypeterminology.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sampletypeterminology.valueholder.SampleTypeTerminologyMapping;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SampleTypeTerminologyMappingService extends BaseObjectService<SampleTypeTerminologyMapping, String> {

    /** Active terminology mappings for a sample type. */
    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_VIEW')")
    List<SampleTypeTerminologyMapping> getActiveBySampleTypeId(String sampleTypeId);

    /**
     * Reconcile a sample type's terminology mappings to exactly the desired set, in
     * one transaction. Identity is the natural key {@code (source, code)} (which
     * the DB also enforces unique per sample type): a desired mapping whose
     * {@code (source, code)} already exists is updated/reactivated rather than
     * re-inserted — so re-adding a previously-removed code never collides with the
     * unique constraint. Existing active mappings absent from {@code desired} are
     * soft-deleted ({@code is_active = 'N'}).
     */
    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_MANAGE')")
    void saveMappingsForSampleType(String sampleTypeId, List<SampleTypeTerminologyMapping> desired, String sysUserId);

    /**
     * Reconcile a configured LOINC code into a sample type's terminology mappings,
     * so a {@code loinc} column in a sample-types configuration file shows up in
     * the Sample Type Editor as LOINC / SAME_AS.
     *
     * <p>
     * Narrower than {@link #saveMappingsForSampleType}, deliberately: a
     * configuration import knows about one code and must not disturb mappings an
     * administrator added by hand for other systems. A non-blank code upserts its
     * LOINC mapping — reactivating rather than re-inserting, since
     * {@code (sample_type_id, source, code)} is unique — and soft-deletes any other
     * active LOINC mapping carrying a different code. A blank code leaves
     * everything alone: an import that omits the column is saying nothing about
     * LOINC, not asking for it to be cleared.
     */
    void syncConfiguredLoinc(String sampleTypeId, String loinc, String sysUserId);
}
