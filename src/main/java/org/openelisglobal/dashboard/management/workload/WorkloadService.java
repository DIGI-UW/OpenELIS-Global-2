package org.openelisglobal.dashboard.management.workload;

import org.openelisglobal.dashboard.management.ScopeRange;

public interface WorkloadService {

    WorkloadResponse getWorkload(ScopeRange scope);
}
