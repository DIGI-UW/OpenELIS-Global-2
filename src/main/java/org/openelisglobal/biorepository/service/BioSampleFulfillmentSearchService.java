package org.openelisglobal.biorepository.service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Phased exact-first search for biorepository fulfillment (Stage 5).
 */
public interface BioSampleFulfillmentSearchService {

    /**
     * Run waterfall fulfillment search: exact accession, exact barcode, partial identity,
     * type+origin, then project.
     */
    FulfillmentSearchOutcome search(FulfillmentSearchInput input, HttpServletRequest request);
}
