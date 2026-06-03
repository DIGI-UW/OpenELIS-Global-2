package org.openelisglobal.vector.deconvolution.dto;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public final class DeconvolutionDTOs {

    private DeconvolutionDTOs() {
    }

    /** One panel with the tests inside it that can be added to sub-pools. */
    @Getter
    @RequiredArgsConstructor
    public static class PanelTestGroup {
        private final String panelId;
        private final String panelName;
        private final List<PanelTestEntry> tests;

        @Getter
        @RequiredArgsConstructor
        public static class PanelTestEntry {
            private final String testId;
            private final String testName;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DeconvolutionInitiateRequest {
        private Long vectorPoolId;
        private int poolCount;
        private int organismsPerPool;
        private String notes;
        private Map<Integer, Long> subPoolLocationIds;
        private Map<Integer, String> subPoolNotes;
        private Map<Long, Integer> memberAssignments;
        private String assignmentStrategy;
        /**
         * Optional. When provided, only analyses whose test ID appears in this list are
         * copied to sub-pools. Tests in this list that do not yet exist as pool
         * analyses are created fresh on each sub-pool. When null/empty the default
         * behaviour (copy all unconfirmed pool analyses) is used.
         */
        private List<String> selectedTestIds;
    }

    @Getter
    @RequiredArgsConstructor
    public static class PoolResultSummary {
        private final String testName;
        private final String resultDisplay;
        private final String analysisId;
        private final boolean confirmedForAllMembers;
    }

    @Getter
    @RequiredArgsConstructor
    public static class DeconvolutionNode {
        private final Long vectorPoolId;
        private final String externalIdLabel;
        private final Long parentPoolId;
        private final Integer memberCount;

        @Setter
        private List<PoolResultSummary> results;
    }

    @Getter
    public static class DeconvolutionPreview {
        /** All pool-level tests — confirmed and unconfirmed. */
        private final List<PoolTestEntry> poolTests;

        public DeconvolutionPreview(List<PoolTestEntry> poolTests) {
            this.poolTests = poolTests != null ? poolTests : java.util.List.of();
        }

        /**
         * One entry per pool-level test. confirmedForAll = true → already confirmed for
         * every member (available to re-add). confirmedForAll = false → still needs
         * sub-pool resolution (pre-selected by default).
         */
        @Getter
        @RequiredArgsConstructor
        public static class PoolTestEntry {
            private final String testId;
            private final String testName;
            private final boolean confirmedForAll;
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class DeconvolutionResult {
        private final Long parentSampleId;
        private final Long vectorPoolId;
        private final List<Long> childSampleItemIds;
        private final List<String> childExternalIds;
        private final int testOrdersCreated;
        private final String newDeconvolutionStatus;

        @Setter
        private List<Long> childPoolIds;

        @Setter
        private Double deconvolutionOutcomePct;

        @Setter
        private Integer leafTotalCount;

        @Setter
        private Integer leafPositiveCount;

        @Setter
        private List<DeconvolutionNode> tree;

        public int getAliquotCount() {
            return childSampleItemIds == null ? 0 : childSampleItemIds.size();
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class DeconvolutionOutcome {
        private final Long parentSampleId;
        private final int confirmedCount;
        private final int totalChildCount;

        public double getOutcomePct() {
            return totalChildCount == 0 ? 0.0 : ((double) confirmedCount / totalChildCount) * 100.0;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DeconvolutionWorklistRowDTO {
        private Long sampleId;
        private Long vectorPoolId;
        private String accessionNumber;
        private String samplingSiteName;
        private String positiveTestName;
        private int childCount;
        private int doneCount;
        private int positiveCount;
        private String deconvolutionStatus;
        private Double deconvolutionOutcomePct;
    }
}
