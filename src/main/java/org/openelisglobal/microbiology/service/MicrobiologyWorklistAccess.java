package org.openelisglobal.microbiology.service;

import java.util.Set;

public record MicrobiologyWorklistAccess(boolean allUnits, Set<String> unitIds) {
    public MicrobiologyWorklistAccess {
        unitIds = Set.copyOf(unitIds);
    }

    public boolean hasAccess() {
        return allUnits || !unitIds.isEmpty();
    }
}
