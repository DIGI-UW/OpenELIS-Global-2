package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.MicroReagentRequirementForm;
import org.openelisglobal.microbiology.form.MicroReagentUsageForm;
import org.openelisglobal.microbiology.valueholder.MicroInventoryUsageContext;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroReagentLotService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroReagentRequirementForm> getRequirements(String caseId);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroReagentUsageForm> getUsageHistory(String caseId);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    void recordSelections(String caseId, MicroInventoryUsageContext context, String actionId,
            List<MicroLotSelection> selections, String performedBy);
}
