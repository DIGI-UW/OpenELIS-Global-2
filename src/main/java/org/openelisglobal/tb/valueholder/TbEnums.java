package org.openelisglobal.tb.valueholder;

/**
 * Enumeration types for TB Laboratory workflow entities.
 */
public class TbEnums {

    private TbEnums() {
        // Utility class
    }

    /**
     * Specimen types accepted for TB testing.
     */
    public enum SpecimenType {
        SPUTUM, BODY_FLUID, SWAB, TISSUE, OTHER
    }

    /**
     * Overall QC result outcomes.
     */
    public enum QcResult {
        /** Sample passed QC, proceed to processing */
        PASS,
        /** Sample passed QC, route to storage */
        PASS_TO_STORAGE,
        /** Sample failed QC, discard */
        FAIL_DISCARD,
        /** Sample failed QC but proceed with remarks */
        FAIL_PROCEED
    }

    /**
     * Rejection reasons for failed QC.
     */
    public enum RejectionReason {
        MISLABELING, INSUFFICIENT_SAMPLE, CONTAMINATED, TEMPERATURE_DEVIATION, PACKAGING_ISSUE, REQUEST_MISMATCH, OTHER
    }

    /**
     * Destination routing after QC.
     */
    public enum QcDestination {
        PROCESSING, TEMPORARY_STORAGE, SHIPMENT, LONG_TERM_STORAGE, DISCARDED
    }

    /**
     * Culture method types.
     */
    public enum CultureMethod {
        /** Lowenstein-Jensen solid culture */
        LJ,
        /** Mycobacteria Growth Indicator Tube liquid culture */
        MGIT,
        /** Both LJ and MGIT */
        BOTH
    }

    /**
     * Culture growth observation results.
     */
    public enum GrowthObservation {
        NO_GROWTH, GROWTH_DETECTED, CONTAMINATED
    }

    /**
     * Smear microscopy methods.
     */
    public enum SmearMethod {
        ZN, CONCENTRATED, FLUORESCENT, OTHER
    }

    /**
     * AFB (Acid-Fast Bacilli) result grading.
     */
    public enum AfbResult {
        NEGATIVE, SCANTY, PLUS1, PLUS2, PLUS3
    }

    /**
     * Species identification results.
     */
    public enum IdentificationResult {
        /** Mycobacterium tuberculosis complex */
        MTB,
        /** Non-tuberculous Mycobacteria */
        NTM, NEGATIVE, CONTAMINATED
    }

    /**
     * Species identification methods.
     */
    public enum IdentificationMethod {
        SMEAR_MORPHOLOGY, BHI_BA, RAPID_TEST_KIT
    }

    /**
     * GeneXpert molecular test results.
     */
    public enum GeneXpertResult {
        MTB_NOT_DETECTED, MTB_RIF_SENSITIVE, MTB_RIF_RESISTANT, MTB_RIF_INDETERMINATE
    }

    /**
     * GeneXpert and molecular test methods.
     */
    public enum MolecularMethod {
        GENEXPERT, REALTIME_PCR, OTHER
    }

    /**
     * DST method types.
     */
    public enum DstMethod {
        PHENOTYPIC_1ST, PHENOTYPIC_2ND, MOLECULAR_1ST
    }

    /**
     * Drug susceptibility result for individual drugs.
     */
    public enum DrugSusceptibility {
        /** Drug sensitive */
        SENSITIVE,
        /** Drug resistant */
        RESISTANT,
        /** Invalid/inconclusive result */
        INVALID
    }

    // ====== Stage 4: Initial Processing & Incubation Enums ======

    /**
     * QC status for prepared media batches.
     */
    public enum MediaQcStatus {
        /** QC pending */
        PENDING,
        /** QC passed, media usable */
        PASSED,
        /** QC failed, media unusable */
        FAILED
    }

    /**
     * Decontamination methods for sample processing.
     */
    public enum DecontaminationMethod {
        /** NALC-NaOH method (standard) */
        NALC_NAOH,
        /** NaOH only method */
        NAOH_ONLY,
        /** Other method */
        OTHER
    }

    /**
     * Sample processing status tracking.
     */
    public enum ProcessingStatus {
        /** Processing not yet started */
        PENDING,
        /** Sample has been decontaminated */
        PROCESSED,
        /** Ready for inoculation to media */
        READY_FOR_INOCULATION
    }

    /**
     * Final culture result after incubation.
     */
    public enum CultureResult {
        /** Growth detected - MTB or NTM positive */
        POSITIVE,
        /** No growth after 8 weeks */
        NEGATIVE,
        /** Culture contaminated */
        CONTAMINATED
    }

    /**
     * Media type for culture preparation and inoculation.
     */
    public enum MediaType {
        /** Lowenstein-Jensen solid culture */
        LJ,
        /** Mycobacteria Growth Indicator Tube liquid culture */
        MGIT
    }

    // ====== Page 7: Disposal & Archiving Enums ======

    /**
     * Sample disposal/archival status.
     */
    public enum DisposalStatus {
        /** Sample pending disposal decision */
        PENDING,
        /** Sample has been disposed */
        DISPOSED,
        /** Sample has been transferred to biorepository/archive */
        ARCHIVED
    }

    /**
     * Reasons for sample disposal in TB workflow.
     */
    public enum DisposalReason {
        /** No growth after 8-week incubation period */
        CULTURE_NEGATIVE,
        /** All requested tests have been completed */
        TESTING_COMPLETE,
        /** Sample or culture contaminated */
        CONTAMINATED,
        /** Sample no longer viable for testing */
        DEGRADED,
        /** Storage capacity limit reached */
        STORAGE_LIMIT,
        /** Redundant sample from same patient */
        DUPLICATE,
        /** Sample beyond usable timeframe */
        EXPIRED,
        /** Other reason (requires notes) */
        OTHER
    }

    /**
     * Methods for sample disposal in TB laboratory.
     */
    public enum DisposalMethod {
        /** Biohazard autoclave - standard TB decontamination */
        AUTOCLAVE,
        /** High-temperature incineration */
        INCINERATION,
        /** Chemical neutralization/decontamination */
        CHEMICAL,
        /** Transfer to biorepository for long-term archiving */
        BIOREPOSITORY
    }
}
