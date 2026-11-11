package org.openelisglobal.dashboard.management.equipment;

import java.util.List;
import org.openelisglobal.dashboard.management.ScopeRange;

public interface EquipmentService {

    List<AnalyzerActivity> getEquipment(ScopeRange scope);
}
