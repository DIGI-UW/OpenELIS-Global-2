package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerDeliveryAction;

public interface AnalyzerDeliveryActionService {

    String RETRY = "RETRY";
    String DISMISS = "DISMISS";

    /**
     * Retains one manual action on an undelivered analyzer result, so the acting
     * OpenELIS user is durably attributed. Call once the Bridge has accepted the
     * action.
     *
     * @param analyzerId the OpenELIS analyzer, or null when the outbox entry came
     *                   from a sender that matches no analyzer
     */
    AnalyzerDeliveryAction retain(String outboxEntryId, String action, String analyzerId, String actor);

    List<AnalyzerDeliveryAction> findByOutboxEntryId(String outboxEntryId);
}
