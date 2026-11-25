package org.openelisglobal.dashboard.management.surveillance;

import org.openelisglobal.dashboard.management.ScopeRange;

public interface SurveillanceService {

    SurveillanceResponse getSurveillance(ScopeRange scope);
}
