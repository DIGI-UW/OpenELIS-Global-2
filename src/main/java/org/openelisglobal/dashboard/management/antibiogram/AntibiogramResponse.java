package org.openelisglobal.dashboard.management.antibiogram;

import java.util.List;

public record AntibiogramResponse(List<Organism> organisms, List<Antibiotic> antibiotics, List<Cell> cells) {

    public record Organism(String id, String name, int isolates) {
    }

    public record Antibiotic(String id, String name) {
    }

    // pctR is null below the minimum number of isolates tested.
    public record Cell(String organismId, String antibioticId, int n, Double pctR) {
    }
}
