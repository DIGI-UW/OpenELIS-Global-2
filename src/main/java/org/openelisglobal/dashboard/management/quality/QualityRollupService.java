package org.openelisglobal.dashboard.management.quality;

import org.openelisglobal.dashboard.management.ScopeRange;

public interface QualityRollupService {

    QualityRollup getRollup(ScopeRange scope);
}
