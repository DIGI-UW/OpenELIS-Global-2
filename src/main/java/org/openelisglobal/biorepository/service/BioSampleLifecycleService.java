package org.openelisglobal.biorepository.service;

import java.util.List;
import org.openelisglobal.biorepository.controller.rest.dto.BioSampleLifecycleEventDTO;

/**
 * Aggregates a chronological lifecycle trail for one {@link BioSample} from existing
 * storage, transfer, and retrieval persistence.
 */
public interface BioSampleLifecycleService {

    /**
     * Build ordered lifecycle events for the given BioSample ID.
     *
     * @param bioSampleId biorepository extension id
     * @return events sorted by time ascending; empty if none
     */
    List<BioSampleLifecycleEventDTO> buildLifecycleEvents(Integer bioSampleId);
}
