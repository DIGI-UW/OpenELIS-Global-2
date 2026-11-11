package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;

public interface MicrobiologyReferenceService {
    List<MicroOrganism> getActiveOrganisms();

    List<MicroAntibiotic> getActiveAntibiotics();

    List<MicroAstPanel> getActiveAstPanels(String organismGroup);

    MicroPatientOriginOptions getPatientOrigins(String organizationId);

    boolean isActivePatientOriginCode(String code);
}
