package org.openelisglobal.eqa.service;

import java.util.List;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;

/**
 * The provider↔participant exchange over the provider's FHIR store (OGC-610 /
 * OGC-613): the participant pushes its DiagnosticReport bundle to the
 * provider's store and later reads score reports from it; the provider reads
 * participant reports from its own store and writes score reports there. The
 * consignment's SupplyDelivery uuid — held by both instances since the shipment
 * loop — is the identity that places every resource; analytes are matched by
 * name because catalog ids differ per install.
 */
public interface EQAFhirExchangeService {

    /**
     * Provider side: apply every participant report in this instance's own store.
     */
    int pollParticipantReports();

    /**
     * Participant side: apply every score report addressed to this lab's
     * consignments.
     */
    int pollScoreReports();

    /**
     * Provider side: take in one participant report. Idempotent — values already on
     * file are left alone and the call answers false.
     */
    boolean applyParticipantReport(DiagnosticReport report, List<Observation> observations);

    /**
     * Participant side: apply one score report to the local cycle its consignment
     * belongs to. Answers false when the cycle is already scored or nothing
     * matched.
     */
    boolean applyScoreReport(DiagnosticReport report, List<Observation> observations);
}
