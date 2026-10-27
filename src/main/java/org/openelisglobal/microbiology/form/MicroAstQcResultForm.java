package org.openelisglobal.microbiology.form;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MicroAstQcResultForm {
    public boolean passed;
    public List<Row> rows = new ArrayList<>();

    public static class Row {
        public String antibioticId;
        public BigDecimal low;
        public BigDecimal high;
        public BigDecimal measuredValue;
        public String outcome;
        public String qcResultId;
    }
}
