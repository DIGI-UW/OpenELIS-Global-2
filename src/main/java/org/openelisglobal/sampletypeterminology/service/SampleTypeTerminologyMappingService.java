package org.openelisglobal.sampletypeterminology.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sampletypeterminology.valueholder.SampleTypeTerminologyMapping;

public interface SampleTypeTerminologyMappingService extends BaseObjectService<SampleTypeTerminologyMapping, String> {

    /** Active terminology mappings for a sample type. */
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
    void saveMappingsForSampleType(String sampleTypeId, List<SampleTypeTerminologyMapping> desired, String sysUserId);
}
