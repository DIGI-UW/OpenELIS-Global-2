package org.openelisglobal.containertype.form;

import java.util.List;

/**
 * The samples Prepare Samples asks containers for: each with its sample type,
 * its ordered tests, and the panels they were ordered through. A test belongs
 * to the first listed panel that contains it.
 */
public record ContainerProposalForm(List<Sample> samples) {

    public record Sample(String key, String sampleTypeId, List<String> testIds, List<String> panelIds) {
    }
}
