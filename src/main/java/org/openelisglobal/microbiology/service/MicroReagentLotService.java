package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.MicroReagentRequirementForm;
import org.openelisglobal.microbiology.form.MicroReagentUsageForm;
import org.openelisglobal.microbiology.valueholder.MicroInventoryUsageContext;

public interface MicroReagentLotService {

    List<MicroReagentRequirementForm> getRequirements(String caseId);

    List<MicroReagentUsageForm> getUsageHistory(String caseId);

    void recordSelections(String caseId, MicroInventoryUsageContext context, String actionId,
            List<MicroLotSelection> selections, String performedBy);
}
