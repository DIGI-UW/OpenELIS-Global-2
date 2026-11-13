package org.openelisglobal.containertype.form;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.common.util.IdValuePair;

/**
 * A test's Containers section (FR-G5 to G6b): its expected containers, its
 * primary and secondary sample types, and the containers it can choose from,
 * those yielding its sample types first and those yielding another sample type
 * second.
 */
public record TestContainersView(String testId, String domain, boolean needsOwnContainer, List<Row> containers,
        List<IdValuePair> primarySampleTypes, List<SecondarySampleType> secondarySampleTypes,
        List<ContainerTypeView> yieldingChoices, List<ContainerTypeView> otherChoices) {

    public record Row(Long containerTypeId, String code, String name, String capColourHex, String capColourName,
            boolean active, String role, int count, BigDecimal minVolume, String volumeUnit, String usedAsSampleTypeId,
            String usedAsSampleTypeName, List<IdValuePair> yields) {
    }

    /**
     * A sample type the test accepts through a container used as its own (FR-G6b).
     */
    public record SecondarySampleType(String sampleTypeId, String sampleTypeName, String containerName,
            String usedAsSampleTypeName) {
    }
}
