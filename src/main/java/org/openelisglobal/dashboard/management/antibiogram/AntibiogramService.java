package org.openelisglobal.dashboard.management.antibiogram;

import org.openelisglobal.dashboard.management.ScopeRange;

public interface AntibiogramService {

    AntibiogramResponse getAntibiogram(ScopeRange scope);
}
