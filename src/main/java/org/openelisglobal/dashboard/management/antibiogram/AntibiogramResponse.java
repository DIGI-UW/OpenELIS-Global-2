package org.openelisglobal.dashboard.management.antibiogram;

import java.util.List;

// fallback: no reportable reading in scope, so only organism counts apply.
public record AntibiogramResponse(List<Organism> organisms, List<Antibiotic> antibiotics, List<Cell> cells,
        boolean fallback) {

    public record Organism(String id, String name, int isolates) {
    }

    public record Antibiotic(String id, String name) {
    }

    // pctR is null below the minimum number of isolates tested.
    public record Cell(String organismId, String antibioticId, int n, Double pctR) {
    }
}
