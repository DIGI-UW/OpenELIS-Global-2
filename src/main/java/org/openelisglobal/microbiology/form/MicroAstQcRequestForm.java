package org.openelisglobal.microbiology.form;

import java.math.BigDecimal;
import java.util.List;

public class MicroAstQcRequestForm {
    public String controlLotId;
    public List<Long> lotIds = List.of();
    public List<Measurement> results = List.of();

    public static class Measurement {
        public String antibioticId;
        public BigDecimal measuredValue;
    }
}
