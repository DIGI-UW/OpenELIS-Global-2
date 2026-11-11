package org.openelisglobal.microbiology.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The same membership-first decision is used by saving and unsaved previews.
 */
public final class MicroCaseRoutingRule {
    private MicroCaseRoutingRule() {
    }

    public static final class Candidate {
        public final String caseId;
        public final String labUnitId;
        public final String sampleTypeId;
        public final String siteId;
        public final Set<String> samples = new LinkedHashSet<>();
        public final Set<String> setsTests = new LinkedHashSet<>();

        public Candidate(String caseId, String labUnitId, String sampleTypeId) {
            this(caseId, labUnitId, sampleTypeId, null);
        }

        public Candidate(String caseId, String labUnitId, String sampleTypeId, String siteId) {
            this.siteId = siteId;
            this.caseId = caseId;
            this.labUnitId = labUnitId;
            this.sampleTypeId = sampleTypeId;
        }
    }

    public static void requireCatalog(org.openelisglobal.test.valueholder.Test test) {
        if (test == null || !test.isOpensMicrobiologyCase() || test.getTestSection() == null
                || test.getTestSection().getId() == null || !test.isActive()
                || !"Y".equals(test.getTestSection().getIsActive())) {
            throw new IllegalArgumentException("An active micro test and lab unit are required");
        }
        var role = org.openelisglobal.microbiology.valueholder.MicroCaseRole.valueOf(test.getMicrobiologyCaseRole());
        if (test.isCollectedInSets() && role != org.openelisglobal.microbiology.valueholder.MicroCaseRole.CULTURE) {
            throw new IllegalArgumentException("Only culture tests are collected in sets");
        }
    }

    public static Candidate choose(List<Candidate> oldestFirst, String labUnitId, String sampleTypeId,
            String setsTestId, String sampleKey) {
        return choose(oldestFirst, labUnitId, sampleTypeId, setsTestId, sampleKey, null);
    }

    public static Candidate choose(List<Candidate> oldestFirst, String labUnitId, String sampleTypeId,
            String setsTestId, String sampleKey, String siteId) {
        for (Candidate candidate : oldestFirst) {
            if (Objects.equals(labUnitId, candidate.labUnitId) && candidate.samples.contains(sampleKey)) {
                return candidate;
            }
        }
        for (Candidate candidate : oldestFirst) {
            if (Objects.equals(labUnitId, candidate.labUnitId) && (setsTestId == null
                    ? Objects.equals(sampleTypeId, candidate.sampleTypeId) && Objects.equals(siteId, candidate.siteId)
                    : candidate.setsTests.contains(setsTestId))) {
                return candidate;
            }
        }
        return null;
    }
}
