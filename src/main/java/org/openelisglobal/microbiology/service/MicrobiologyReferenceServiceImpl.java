package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.dao.MicroPatientOriginDAO;
import org.openelisglobal.microbiology.dao.MicroPatientOriginDefaultDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicrobiologyReferenceServiceImpl implements MicrobiologyReferenceService {

    private final MicroOrganismDAO organismDAO;
    private final MicroAntibioticDAO antibioticDAO;
    private final MicroAstPanelDAO astPanelDAO;
    private final MicroPatientOriginDAO patientOriginDAO;
    private final MicroPatientOriginDefaultDAO patientOriginDefaultDAO;

    public MicrobiologyReferenceServiceImpl(MicroOrganismDAO organismDAO, MicroAntibioticDAO antibioticDAO,
            MicroAstPanelDAO astPanelDAO, MicroPatientOriginDAO patientOriginDAO,
            MicroPatientOriginDefaultDAO patientOriginDefaultDAO) {
        this.organismDAO = organismDAO;
        this.antibioticDAO = antibioticDAO;
        this.astPanelDAO = astPanelDAO;
        this.patientOriginDAO = patientOriginDAO;
        this.patientOriginDefaultDAO = patientOriginDefaultDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroOrganism> getActiveOrganisms() {
        return organismDAO.getActiveOrganisms();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAntibiotic> getActiveAntibiotics() {
        return antibioticDAO.getActiveAntibiotics();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstPanel> getActiveAstPanels(String organismGroup) {
        return astPanelDAO.getActivePanelsByOrganismGroup(organismGroup);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroPatientOriginOptions getPatientOrigins(String organizationId) {
        var options = patientOriginDAO.getActivePatientOrigins();
        String defaultId = organizationId == null || organizationId.isBlank() ? null
                : patientOriginDefaultDAO.findPatientOriginIdByOrganizationId(organizationId);
        String defaultCode = options.stream().filter(option -> option.getId().equals(defaultId))
                .map(option -> option.getCode()).findFirst().orElse(null);
        return new MicroPatientOriginOptions(options, defaultCode);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActivePatientOriginCode(String code) {
        return patientOriginDAO.existsActiveCode(code);
    }
}
