package org.openelisglobal.dashboard.management.export;

import java.util.List;
import java.util.Set;

public record ExportEvent(String kind, String scope, List<SectionRows> sections) {

    static final Set<String> SECTIONS = Set.of("kpis", "workload", "equipment", "antibiogram", "quality", "stock",
            "surveillance");

    public record SectionRows(String id, int rowCount) {
    }

    public boolean isValid() {
        return ("PDF".equals(kind) || "CSV".equals(kind)) && scope != null && sections != null && !sections.isEmpty()
                && sections.stream().allMatch(s -> s != null && SECTIONS.contains(s.id()) && s.rowCount() >= 0)
                && sections.stream().map(SectionRows::id).distinct().count() == sections.size();
    }
}
