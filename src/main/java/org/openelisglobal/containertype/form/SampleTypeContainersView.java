package org.openelisglobal.containertype.form;

import java.util.List;
import org.openelisglobal.common.util.IdValuePair;

/**
 * A sample type's container settings (FR-G7) and the tests it serves (FR-G6b):
 * primary for the tests whose sample type it is, secondary for the tests that
 * accept it through a container used as their own sample type.
 */
public record SampleTypeContainersView(String sampleTypeId, Long defaultContainerTypeId,
        List<ContainerTypeView> containerChoices, List<IdValuePair> primaryForTests,
        List<SecondaryTest> secondaryForTests) {

    public record SecondaryTest(String testId, String testName, String containerName, String usedAsSampleTypeName) {
    }
}
