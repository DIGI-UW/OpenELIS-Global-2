package org.openelisglobal.containertype.form;

import java.math.BigDecimal;
import java.util.List;

/**
 * A test's expected containers as the editor saves them, in order, with whether
 * the test needs its own container (FR-G5, FR-G6).
 */
public record TestContainersForm(List<Row> containers, boolean needsOwnContainer) {

    /**
     * One expected container. {@code usedAsSampleTypeId} names the test's own
     * sample type a container yielding another sample type is used as.
     */
    public record Row(Long containerTypeId, String role, Integer count, BigDecimal minVolume, String volumeUnit,
            String usedAsSampleTypeId) {
    }
}
