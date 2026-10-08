package org.openelisglobal.microbiology.service;

import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;

/** Shared grouping decision for order preview and persistence. */
public record MicroCaseRoutingKey(String sampleId, String sampleTypeId, String testSectionId,
        String collectedInSetsTestId) {

    public static MicroCaseRoutingKey forTest(SampleItem specimen, Test test) {
        if (specimen == null || test == null || !test.isOpensMicrobiologyCase()) {
            throw new IllegalArgumentException("A specimen and a micro test are required");
        }
        MicroCaseRoutingRule.requireCatalog(test);
        MicroCaseServiceImpl.requireText(specimen.getTypeOfSampleId(), "sampleTypeId");
        boolean sets = test.isCollectedInSets();
        if (sets && !"CULTURE".equals(test.getMicrobiologyCaseRole())) {
            throw new IllegalArgumentException("Only a culture test can be collected in sets");
        }
        return new MicroCaseRoutingKey(specimen.getSample() == null ? null : specimen.getSample().getId(),
                sets ? null : specimen.getTypeOfSampleId(), test.getTestSection().getId(), sets ? test.getId() : null);
    }
}
