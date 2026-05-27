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
    }

    @Getter
    @RequiredArgsConstructor
    public static class PoolResultSummary {
        private final String testName;
        private final String resultDisplay;
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
    @RequiredArgsConstructor
    public static class DeconvolutionPreview {
        private final List<String> copiedTests;
        private final List<ReflexEntry> reflexTests;
        /**
         * Rule labels for individual-only rules that would fire if the pool is split to
         * qty=1 sub-pools.
         */
        private final List<String> individualOnlyRuleLabels;

        public DeconvolutionPreview(List<String> copiedTests, List<ReflexEntry> reflexTests) {
            this.copiedTests = copiedTests;
            this.reflexTests = reflexTests;
            this.individualOnlyRuleLabels = java.util.List.of();
        }

        @Getter
        @RequiredArgsConstructor
        public static class ReflexEntry {
            private final String testName;
            private final String ruleLabel;
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
