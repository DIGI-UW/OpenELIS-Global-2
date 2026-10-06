package org.openelisglobal.dictionaryterminology.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dictionaryterminology.valueholder.DictionaryTerminologyMapping;

public interface DictionaryTerminologyMappingService extends BaseObjectService<DictionaryTerminologyMapping, String> {

    /** Active terminology mappings for an answer. */
    List<DictionaryTerminologyMapping> getActiveByDictionaryId(String dictionaryId);

    /**
     * Reconciles an answer's terminology mappings to exactly the desired set in one
     * transaction. Identity is {@code (source, code)}, unique per answer, so a
     * desired mapping that already exists is updated or reactivated rather than
     * inserted again. Active mappings missing from {@code desired} are
     * soft-deleted. Afterwards {@code dictionary.loinc_code} holds the first
     * SAME_AS LOINC code, or is cleared when none remains.
     */
    void saveMappingsForDictionary(String dictionaryId, List<DictionaryTerminologyMapping> desired, String sysUserId);

    /**
     * Brings a code written to the legacy {@code dictionary.loinc_code} column
     * (dictionary CSV, Dictionary Management) into the mappings as LOINC SAME_AS. A
     * non-blank code is upserted, reactivating its row when one exists, and any
     * other active LOINC mapping is soft-deleted; a blank code soft-deletes every
     * active LOINC mapping. Mappings in other systems are never touched.
     */
    void syncLegacyLoinc(String dictionaryId, String loinc, String sysUserId);
}
