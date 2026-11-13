package org.openelisglobal.containertype.form;

import java.math.BigDecimal;
import java.util.List;

/** A container type as the admin page adds or edits it (FR-G2). */
public record ContainerTypeForm(String code, String name, String category, String additive, String material,
        BigDecimal nominalVolume, String volumeUnit, String capColourHex, String capColourName, String standardCode,
        List<String> domains, List<String> sampleTypeIds) {
}
