package org.openelisglobal.microbiology.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** The same membership-first decision is used by saving and unsaved previews. */
public final class MicroCaseRoutingRule {
    private MicroCaseRoutingRule() {}

    public static final class Candidate {
        public final String caseId;
        public final String labUnitId;
        public final String sampleTypeId;
        public final Set<String> samples = new LinkedHashSet<>();
        public final Set<String> setsTests = new LinkedHashSet<>();

        public Candidate(String caseId, String labUnitId, String sampleTypeId) {
            this.caseId = caseId;
            this.labUnitId = labUnitId;
            this.sampleTypeId = sampleTypeId;
        }
    }

    public static Candidate choose(List<Candidate> oldestFirst, String labUnitId, String sampleTypeId,
            String setsTestId, String sampleKey) {
        for (Candidate candidate : oldestFirst) {
            if (Objects.equals(labUnitId, candidate.labUnitId) && candidate.samples.contains(sampleKey)) {
                return candidate;
            }
        }
        for (Candidate candidate : oldestFirst) {
            if (Objects.equals(labUnitId, candidate.labUnitId)
                    && (setsTestId == null ? Objects.equals(sampleTypeId, candidate.sampleTypeId)
                            : candidate.setsTests.contains(setsTestId))) {
                return candidate;
            }
        }
        return null;
    }
}
