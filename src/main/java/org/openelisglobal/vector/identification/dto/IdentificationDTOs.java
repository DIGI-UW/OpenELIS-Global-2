package org.openelisglobal.vector.identification.dto;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.openelisglobal.vector.identification.valueholder.VectorSpecimenIdentification;

public final class IdentificationDTOs {

    private IdentificationDTOs() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class BulkIdentifyRequest {
        private List<Long> sampleItemIds;
        private Long vectorSpeciesId;
        private String identificationMethod;
        private String confidence;
        private String notes;
        private String physiologicalState;
        private String lifecycleStage;
    }

    @Getter
    @RequiredArgsConstructor
    public static class BulkIdentifyResult {
        private final List<VectorSpecimenIdentification> identifications;
        private final Map<Long, String> sampleIdentificationStatuses;
        private final boolean bloodMealSuggested;

        public int getCount() {
            return identifications == null ? 0 : identifications.size();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class IdentificationRequest {
        private Long sampleItemId;
        private Long vectorSpeciesId;
        private String identificationMethod;
        private String confidence;
        private String notes;
        private String physiologicalState;
        private String lifecycleStage;
        private String targetGene;
        private String assayName;
        private String genbankAccession;
        private Long linkedResultId;
    }

    @Getter
    @RequiredArgsConstructor
    public static class IdentificationResult {
        private final VectorSpecimenIdentification identification;
        private final boolean bloodMealSuggested;
        private final String newSampleIdentificationStatus;
    }

    @Getter
    @RequiredArgsConstructor
    public static class LinkedResultCandidateDTO {
        private final Long resultId;
        private final String testName;
        private final String value;
        private final String sampleItemExternalId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class WorklistRowDTO {
        private Long sampleId;
        private Long vectorPoolId;
        /** User-facing lot identifier sourced from {@code vector_pool.external_id}. */
        private String lotExternalId;
        private String accessionNumber;
        private String samplingSiteName;
        private Timestamp collectionDate;
        private String organismGroup;
        private List<String> organismGroups;
        private long totalSpecimens;
        private long identifiedSpecimens;
        private String identificationStatus;
        private String deconvolutionStatus;
        private String positiveTestName;
        private int pendingSubPoolCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SpecimenDetailDTO {
        private Long sampleItemId;
        private String externalId;
        private String sortOrder;
        private Double quantity;
        private Long vectorPoolId;
        private Long parentPoolId;
        private String poolExternalId;
        private String parentPoolExternalId;
        private Long typeOfSampleId;
        private String typeOfSampleName;
        private String poolDeconvolutionStatus;
        private Long identificationId;
        private Long vectorSpeciesId;
        private String identificationMethod;
        private String confidence;
        private String physiologicalState;
        private String lifecycleStage;
        private String identificationStatus;

        public static SpecimenDetailDTO fromIdentification(VectorSpecimenIdentification id) {
            SpecimenDetailDTO dto = new SpecimenDetailDTO();
            if (id == null) {
                dto.setIdentificationStatus("NOT_IDENTIFIED");
                return dto;
            }
            dto.setIdentificationId(id.getId());
            dto.setVectorSpeciesId(id.getVectorSpeciesId());
            dto.setIdentificationMethod(id.getIdentificationMethod());
            dto.setConfidence(id.getConfidence());
            dto.setPhysiologicalState(id.getPhysiologicalState());
            dto.setLifecycleStage(id.getLifecycleStage());
            dto.setIdentificationStatus(id.getConfidence());
            return dto;
        }
    }
}
