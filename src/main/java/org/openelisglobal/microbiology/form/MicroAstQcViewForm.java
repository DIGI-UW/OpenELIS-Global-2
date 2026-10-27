package org.openelisglobal.microbiology.form;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MicroAstQcViewForm {
    public boolean configured;
    public boolean canRecord;
    public List<Antibiotic> antibiotics = new ArrayList<>();
    public List<ControlLot> controlLots = new ArrayList<>();
    public List<Lot> lots = new ArrayList<>();
    public List<Lot> candidateLots = new ArrayList<>();
    public List<Control> controls = new ArrayList<>();

    public static class Antibiotic {
        public String id;
        public String name;
    }

    public static class ControlLot {
        public String id;
        public String label;
        public Map<String, Range> ranges = new LinkedHashMap<>();
    }

    public static class Range {
        public BigDecimal low;
        public BigDecimal high;
    }

    public static class Lot {
        public Long id;
        public String lotNumber;
        public String itemName;
        public String qcStatus;
    }

    public static class Control {
        public String controlId;
        public String controlLotLabel;
        public Timestamp recordedAt;
        public boolean passed;
        public List<MicroAstQcResultForm.Row> rows = new ArrayList<>();
    }
}
