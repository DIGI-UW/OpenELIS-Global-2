package org.openelisglobal.analyzerresults.service;

import java.util.List;

/**
 * Where an analyzer result would land, and why. Everything but {@code RESOLVED}
 * needs the reviewer to look; nothing is chosen on their behalf.
 */
public record AnalyzerResultPlacement(State state, Match match, String accessionNumber, List<Tube> tubes,
        List<Candidate> analyses, String proposedSampleItemId, String proposedAnalysisId, PatientCheck patient) {

    public enum State {
        /** One analysis awaits this result. Safe to pre-tick. */
        RESOLVED,
        /** The one matching analysis already holds an accepted or final result. */
        RETEST_CHOICE,
        /** More than one analysis (or tube) matches. */
        MULTI_TUBE,
        /** The test is not ordered; exactly one tube can hold it. */
        UNORDERED_ONE_FITS,
        /** The test is not ordered; several tubes can hold it. */
        UNORDERED_MANY_FIT,
        /** The test is not ordered; no tube can hold it. */
        UNORDERED_NONE_FIT,
        /** No order or tube carries the instrument's ID. */
        NEW_SAMPLE
    }

    public enum Match {
        TUBE, ACCESSION, NONE
    }

    /** What the instrument said about the patient, against the order's patient. */
    public enum PatientStatus {
        /** The instrument sent no patient. */
        NOT_REPORTED,
        /** The instrument's patient ID is one of the order patient's identifiers. */
        MATCH,
        /**
         * The order has a patient and the instrument's ID is none of their identifiers.
         */
        MISMATCH,
        /** The order has no patient identifier to compare with. */
        NO_ORDER_PATIENT
    }

    public record PatientCheck(PatientStatus status, String instrumentId, String instrumentName, String orderName) {
        public static final PatientCheck NOT_REPORTED = new PatientCheck(PatientStatus.NOT_REPORTED, null, null, null);
    }

    public record Tube(String sampleItemId, String externalId, String typeOfSampleId) {
    }

    public record Candidate(String analysisId, String sampleItemId, String statusId, boolean awaitingResult) {
    }

    public AnalyzerResultPlacement {
        tubes = tubes == null ? List.of() : List.copyOf(tubes);
        analyses = analyses == null ? List.of() : List.copyOf(analyses);
        patient = patient == null ? PatientCheck.NOT_REPORTED : patient;
    }
}
