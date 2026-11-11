package org.openelisglobal.dashboard.management.workload;

import java.math.BigDecimal;
import java.util.List;

public record WorkloadResponse(List<String> days, List<Section> sections) {

    // TAT figures are in hours over validated analyses; null when the section has
    // none. onTimePct is also null when the TAT indicator is turned off.
    public record Section(String id, String name, List<Long> counts, long samples, BigDecimal medianTat,
            BigDecimal p90Tat, BigDecimal onTimePct) {
    }
}
