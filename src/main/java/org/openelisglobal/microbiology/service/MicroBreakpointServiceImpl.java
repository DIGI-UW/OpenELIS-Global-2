package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.dao.MicroBreakpointRuleDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.form.MicroBreakpointDefaultForm;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroBreakpointServiceImpl implements MicroBreakpointService {

    public static final String DEFAULT_AUTHORITY = "microbiologyDefaultBreakpointAuthority";
    public static final String DEFAULT_VERSION = "microbiologyDefaultBreakpointVersion";

    private final SiteInformationService settings;
    private final MicroBreakpointStandardDAO standardDAO;
    private final MicroBreakpointRuleDAO ruleDAO;

    public MicroBreakpointServiceImpl(MicroBreakpointStandardDAO standardDAO, MicroBreakpointRuleDAO ruleDAO,
            SiteInformationService settings) {
        this.settings = settings;
        this.standardDAO = standardDAO;
        this.ruleDAO = ruleDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointDefaultForm getDefaultStandard() {
        MicroBreakpointDefaultForm form = new MicroBreakpointDefaultForm();
        form.authority = setting(DEFAULT_AUTHORITY);
        form.version = setting(DEFAULT_VERSION);
        form.configured = !form.authority.isBlank() || !form.version.isBlank();
        if (form.configured) {
            standardDAO.findByAuthorityAndVersion(form.authority, form.version).ifPresent(standard -> {
                form.standardId = standard.getId();
                form.available = "Y".equals(standard.getIsActive()) && "ACTIVE".equals(standard.getLifecycleStatus());
            });
        }
        return form;
    }

    @Override
    @Transactional
    public MicroBreakpointDefaultForm setDefaultStandard(String standardId, String actorId) {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("authenticated actor is required");
        }
        MicroBreakpointStandard standard = getStandard(standardId);
        if (standard == null || !"Y".equals(standard.getIsActive())
                || !"ACTIVE".equals(standard.getLifecycleStatus())) {
            throw new IllegalArgumentException("AST_BREAKPOINT_STANDARD_NOT_AVAILABLE");
        }
        saveSetting(DEFAULT_AUTHORITY, standard.getAuthority(), actorId);
        saveSetting(DEFAULT_VERSION, standard.getVersion(), actorId);
        return getDefaultStandard();
    }

    private String setting(String name) {
        SiteInformation value = settings.getSiteInformationByName(name);
        return value == null || value.getValue() == null ? "" : value.getValue().trim();
    }

    private void saveSetting(String name, String value, String actorId) {
        SiteInformation setting = settings.getSiteInformationByName(name);
        boolean isNew = setting == null;
        if (isNew) {
            setting = new SiteInformation();
            setting.setName(name);
            setting.setValueType("text");
        }
        setting.setValue(value);
        setting.setSysUserId(actorId);
        settings.persistData(setting, isNew);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointStandard getActiveStandard(String authority, String version) {
        return standardDAO.getActiveStandard(authority, version);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroBreakpointStandard> getActiveStandards() {
        return standardDAO.getActiveStandards();
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointStandard getStandard(String standardId) {
        return standardDAO.get(standardId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointRule findBreakpointRule(String standardId, String organismId, String organismGroup,
            String antibioticId, String method, String specimenTypeId, String breakpointType) {
        return ruleDAO.findBestRule(standardId, organismId, organismGroup, antibioticId, method, specimenTypeId,
                breakpointType);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroBreakpointRule findContextualBreakpointRule(String standardId, String organismId, String organismGroup,
            String antibioticId, String method, String specimenTypeId, String breakpointType, String clinicalContext,
            String diskContent) {
        return ruleDAO.findContextualRule(standardId, organismId, organismGroup, antibioticId, method, specimenTypeId,
                breakpointType, MicroBreakpointClinicalContext.validate(clinicalContext), diskContent);
    }
}
