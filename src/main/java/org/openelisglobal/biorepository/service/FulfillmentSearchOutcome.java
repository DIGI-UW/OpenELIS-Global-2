package org.openelisglobal.biorepository.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.biorepository.controller.rest.dto.BioSampleListDTO;

/**
 * Result of phased fulfillment search including ranked candidates and metadata.
 */
public class FulfillmentSearchOutcome {

    private List<BioSampleListDTO> candidates = new ArrayList<>();
    private FulfillmentSearchMetadata metadata = new FulfillmentSearchMetadata();

    public List<BioSampleListDTO> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<BioSampleListDTO> candidates) {
        this.candidates = candidates != null ? candidates : new ArrayList<>();
    }

    public FulfillmentSearchMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(FulfillmentSearchMetadata metadata) {
        this.metadata = metadata != null ? metadata : new FulfillmentSearchMetadata();
    }
}
