package org.openelisglobal.notebook.service;

import java.util.List;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Service for creating TB samples in isolated transactions. Uses REQUIRES_NEW
 * propagation to allow individual row failures without affecting other rows
 * during manifest import.
 */
public interface TBSampleCreationService {

    /**
     * Result of creating samples for a single manifest row.
     */
    record RowCreationResult(boolean success, List<SampleItem> createdSamples, List<String> accessionNumbers,
            String errorMessage) {
    }

    /**
     * Create samples for a single manifest row in its own transaction. If this
     * transaction fails, it will not affect the parent transaction.
     *
     * @param entryId   the notebook entry ID
     * @param row       the manifest row data
     * @param sysUserId the user creating the samples
     * @return result with created samples or error message
     */
    RowCreationResult createSamplesForRow(Integer entryId, TBManifestImportService.TBManifestRow row, String sysUserId);
}
