package org.openelisglobal.microbiology.service;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointRuleDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.dao.MicroCultureSetupDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroAstPanelAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicrobiologyConfigurationServiceImpl implements MicrobiologyConfigurationService {

    private final MicroOrganismDAO organismDAO;
    private final MicroAntibioticDAO antibioticDAO;
    private final MicroAstPanelDAO astPanelDAO;
    private final MicroAstPanelAntibioticDAO panelAntibioticDAO;
    private final MicroBreakpointStandardDAO standardDAO;
    private final MicroBreakpointRuleDAO ruleDAO;
    private final MicroCultureSetupDAO cultureSetupDAO;

    public MicrobiologyConfigurationServiceImpl(MicroOrganismDAO organismDAO, MicroAntibioticDAO antibioticDAO,
            MicroAstPanelDAO astPanelDAO, MicroAstPanelAntibioticDAO panelAntibioticDAO,
            MicroBreakpointStandardDAO standardDAO, MicroBreakpointRuleDAO ruleDAO,
            MicroCultureSetupDAO cultureSetupDAO) {
        this.organismDAO = organismDAO;
        this.antibioticDAO = antibioticDAO;
        this.astPanelDAO = astPanelDAO;
        this.panelAntibioticDAO = panelAntibioticDAO;
        this.standardDAO = standardDAO;
        this.ruleDAO = ruleDAO;
        this.cultureSetupDAO = cultureSetupDAO;
    }

    @Override
    @Transactional
    public MicroOrganism createOrganism(MicroOrganism organism) {
        requireText(organism == null ? null : organism.getDisplayName(), "organism.displayName");
        organismDAO.insert(organism);
        return organism;
    }

    @Override
    @Transactional
    public MicroAntibiotic createAntibiotic(MicroAntibiotic antibiotic) {
        requireText(antibiotic == null ? null : antibiotic.getDisplayName(), "antibiotic.displayName");
        antibioticDAO.insert(antibiotic);
        return antibiotic;
    }

    @Override
    @Transactional
    public MicroAntibiotic getOrCreateAntibiotic(String displayName, String whonetCode, String antibioticClass) {
        requireText(displayName, "antibiotic.displayName");
        requireText(whonetCode, "antibiotic.whonetCode");
        List<MicroAntibiotic> existing = antibioticDAO.getAllMatching(Map.of("whonetCode", whonetCode));
        if (!existing.isEmpty()) {
            MicroAntibiotic antibiotic = existing.get(0);
            if (!"Y".equals(antibiotic.getIsActive())) {
                antibiotic.setIsActive("Y");
                antibioticDAO.update(antibiotic);
            }
            return antibiotic;
        }

        MicroAntibiotic antibiotic = new MicroAntibiotic();
        antibiotic.setDisplayName(displayName);
        antibiotic.setWhonetCode(whonetCode);
        antibiotic.setAntibioticClass(antibioticClass);
        return createAntibiotic(antibiotic);
    }

    @Override
    @Transactional
    public MicroAstPanel createAstPanel(MicroAstPanel panel) {
        requireText(panel == null ? null : panel.getName(), "panel.name");
        requireText(panel == null ? null : panel.getWorkflowType(), "panel.workflowType");
        astPanelDAO.insert(panel);
        return panel;
    }

    @Override
    @Transactional
    public MicroAstPanel getOrCreateAstPanel(String name, String workflowType, String organismGroup) {
        requireText(name, "panel.name");
        requireText(workflowType, "panel.workflowType");
        List<MicroAstPanel> existing = astPanelDAO.getAllMatching(Map.of("name", name, "workflowType", workflowType));
        if (!existing.isEmpty()) {
            MicroAstPanel panel = existing.get(0);
            if (!"Y".equals(panel.getIsActive())) {
                panel.setIsActive("Y");
                astPanelDAO.update(panel);
            }
            return panel;
        }

        MicroAstPanel panel = new MicroAstPanel();
        panel.setName(name);
        panel.setWorkflowType(workflowType);
        panel.setOrganismGroup(organismGroup);
        return createAstPanel(panel);
    }

    @Override
    @Transactional
    public MicroAstPanelAntibiotic addAntibioticToPanel(MicroAstPanelAntibiotic panelAntibiotic) {
        requireText(panelAntibiotic == null ? null : panelAntibiotic.getPanelId(), "panelAntibiotic.panelId");
        requireText(panelAntibiotic == null ? null : panelAntibiotic.getAntibioticId(), "panelAntibiotic.antibioticId");
        panelAntibioticDAO.insert(panelAntibiotic);
        return panelAntibiotic;
    }

    @Override
    @Transactional
    public MicroAstPanelAntibiotic getOrCreatePanelAntibiotic(String panelId, String antibioticId, int displayOrder) {
        requireText(panelId, "panelAntibiotic.panelId");
        requireText(antibioticId, "panelAntibiotic.antibioticId");
        List<MicroAstPanelAntibiotic> existing = panelAntibioticDAO
                .getAllMatching(Map.of("panelId", panelId, "antibioticId", antibioticId));
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        MicroAstPanelAntibiotic panelAntibiotic = new MicroAstPanelAntibiotic();
        panelAntibiotic.setPanelId(panelId);
        panelAntibiotic.setAntibioticId(antibioticId);
        panelAntibiotic.setDisplayOrder(displayOrder);
        return addAntibioticToPanel(panelAntibiotic);
    }

    @Override
    @Transactional
    public MicroBreakpointStandard getOrCreateBreakpointStandard(String authority, String version, Date effectiveDate) {
        requireText(authority, "authority");
        requireText(version, "version");
        List<MicroBreakpointStandard> existing = standardDAO
                .getAllMatching(Map.of("authority", authority, "version", version));
        if (!existing.isEmpty()) {
            MicroBreakpointStandard standard = existing.get(0);
            if (!"Y".equals(standard.getIsActive())) {
                standard.setIsActive("Y");
                standardDAO.update(standard);
            }
            return standard;
        }

        MicroBreakpointStandard standard = new MicroBreakpointStandard();
        standard.setAuthority(authority);
        standard.setVersion(version);
        standard.setEffectiveDate(effectiveDate);
        standardDAO.insert(standard);
        return standard;
    }

    @Override
    @Transactional
    public MicroBreakpointRule createBreakpointRule(MicroBreakpointRule rule) {
        requireText(rule == null ? null : rule.getStandardId(), "rule.standardId");
        requireText(rule == null ? null : rule.getAntibioticId(), "rule.antibioticId");
        requireText(rule == null ? null : rule.getBreakpointType(), "rule.breakpointType");
        ruleDAO.insert(rule);
        return rule;
    }

    @Override
    @Transactional
    public MicroBreakpointRule getOrCreateBreakpointRule(MicroBreakpointRule rule) {
        requireText(rule == null ? null : rule.getStandardId(), "rule.standardId");
        requireText(rule == null ? null : rule.getAntibioticId(), "rule.antibioticId");
        requireText(rule == null ? null : rule.getBreakpointType(), "rule.breakpointType");
        MicroBreakpointRule existing = ruleDAO.findBestRule(rule.getStandardId(), rule.getOrganismId(),
                rule.getOrganismGroup(), rule.getAntibioticId(), rule.getMethod(), rule.getSpecimenTypeId(),
                rule.getBreakpointType());
        if (existing != null) {
            if (!"Y".equals(existing.getIsActive())) {
                existing.setIsActive("Y");
                ruleDAO.update(existing);
            }
            return existing;
        }
        return createBreakpointRule(rule);
    }

    @Override
    @Transactional
    public MicroCultureSetup createCultureSetup(MicroCultureSetup setup) {
        requireText(setup == null ? null : setup.getMethodId(), "setup.methodId");
        requireText(setup == null ? null : setup.getName(), "setup.name");
        requireText(setup == null ? null : setup.getWorkflowType(), "setup.workflowType");
        cultureSetupDAO.insert(setup);
        return setup;
    }

    @Override
    @Transactional
    public MicroCultureSetup getOrCreateCultureSetup(MicroCultureSetup setup) {
        requireText(setup == null ? null : setup.getMethodId(), "setup.methodId");
        requireText(setup == null ? null : setup.getName(), "setup.name");
        requireText(setup == null ? null : setup.getWorkflowType(), "setup.workflowType");

        MicroCultureSetup existing = cultureSetupDAO.getActiveSetupForMethod(setup.getMethodId(),
                setup.getWorkflowType());
        if (existing == null) {
            return createCultureSetup(setup);
        }

        boolean changed = false;
        if (!setup.getName().equals(existing.getName())) {
            existing.setName(setup.getName());
            changed = true;
        }
        if (!sameValue(setup.getMediaDefaults(), existing.getMediaDefaults())) {
            existing.setMediaDefaults(setup.getMediaDefaults());
            changed = true;
        }
        if (!sameValue(setup.getIncubationDefaults(), existing.getIncubationDefaults())) {
            existing.setIncubationDefaults(setup.getIncubationDefaults());
            changed = true;
        }
        if (!sameValue(setup.getAtmosphereDefaults(), existing.getAtmosphereDefaults())) {
            existing.setAtmosphereDefaults(setup.getAtmosphereDefaults());
            changed = true;
        }
        if (changed) {
            cultureSetupDAO.update(existing);
        }
        return existing;
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private boolean sameValue(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }
}
