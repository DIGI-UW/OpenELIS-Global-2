package org.openelisglobal.containertype.form;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.common.util.IdValuePair;

/** One row of the Container Types list (FR-G1). */
public record ContainerTypeView(Long id, String code, String name, String category, String additive, String material,
        BigDecimal nominalVolume, String volumeUnit, String capColourHex, String capColourName, String standardCode,
        boolean active, List<String> domains, List<IdValuePair> sampleTypes, int samplesUsing) {
}
