package org.openelisglobal.analyzerimport.service;

import java.util.Optional;

/**
 * The normalized bundle of each accepted delivery, kept for the life of its
 * receipt.
 */
public interface AnalyzerDeliveryBundleService {

    /** The bundle as OpenELIS received it, serialized as FHIR JSON. */
    Optional<String> getBundle(String receiptId);

    /** The receipt a staged result's source message was accepted under. */
    Optional<String> findReceiptId(String connectionId, String messageId);
}
