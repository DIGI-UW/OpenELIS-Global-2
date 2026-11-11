package org.openelisglobal.dashboard.management.quality;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.dashboard.management.quality.QualityRollup.Component;

public class QualityRollupTest {

    private static Component component(String key, String value, String weight) {
        return new Component(key, value == null ? null : new BigDecimal(value), new BigDecimal(weight));
    }

    @Test
    public void compositeIsTheWeightedMeanOfTheComponents() {
        QualityRollup rollup = QualityRollup
                .of(List.of(component("qcPass", "90", "0.30"), component("eqa", "80", "0.25"),
                        component("nceInverse", "98", "0.25"), component("amendmentInverse", "99", "0.20")), null);

        assertEquals(new BigDecimal("91.3"), rollup.composite());
    }

    @Test
    public void aComponentWithNoDataDropsOutAndTheOthersRenormalise() {
        QualityRollup rollup = QualityRollup
                .of(List.of(component("qcPass", "90", "0.30"), component("eqa", null, "0.25"),
                        component("nceInverse", "98", "0.25"), component("amendmentInverse", "99", "0.20")), null);

        assertEquals(new BigDecimal("95.1"), rollup.composite());
    }

    @Test
    public void withNoDataAtAllThereIsNoComposite() {
        QualityRollup rollup = QualityRollup
                .of(List.of(component("qcPass", null, "0.30"), component("eqa", null, "0.70")), null);

        assertNull(rollup.composite());
    }
}
