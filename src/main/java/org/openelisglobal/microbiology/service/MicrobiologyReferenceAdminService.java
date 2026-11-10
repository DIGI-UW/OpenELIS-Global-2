package org.openelisglobal.microbiology.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.microbiology.form.MicroAntibioticAdminForm;
import org.openelisglobal.microbiology.form.MicroAstPanelAdminForm;
import org.openelisglobal.microbiology.form.MicroOrganismAdminForm;
import org.openelisglobal.microbiology.form.MicroPanelCoverageForm;
import org.openelisglobal.microbiology.form.MicroPatientOriginAdminForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminPageForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminQueryForm;
import org.openelisglobal.microbiology.form.MicroReferenceOptionForm;

public interface MicrobiologyReferenceAdminService {

    MicroReferenceAdminPageForm<MicroOrganismAdminForm> getOrganisms(MicroReferenceAdminQueryForm query);

    MicroOrganismAdminForm getOrganism(String id);

    MicroOrganismAdminForm saveOrganism(String id, MicroOrganismAdminForm request, String actorId);

    MicroOrganismAdminForm setOrganismActive(String id, boolean active, String actorId);

    MicroReferenceAdminPageForm<MicroAntibioticAdminForm> getAntibiotics(MicroReferenceAdminQueryForm query);

    MicroAntibioticAdminForm getAntibiotic(String id);

    MicroAntibioticAdminForm saveAntibiotic(String id, MicroAntibioticAdminForm request, String actorId);

    MicroAntibioticAdminForm setAntibioticActive(String id, boolean active, String actorId);

    MicroReferenceAdminPageForm<MicroAstPanelAdminForm> getAstPanels(MicroReferenceAdminQueryForm query);

    MicroAstPanelAdminForm getAstPanel(String id);

    MicroAstPanelAdminForm createPanel(MicroAstPanelAdminForm request, String actorId);

    MicroAstPanelAdminForm publishPanelVersion(String currentPanelId, MicroAstPanelAdminForm request, String actorId);

    List<MicroPanelCoverageForm> getPanelOrganismCoverage(List<String> antibioticIds, String protocol);

    /**
     * Fails when a defaultFor entry of a panel in {@code lines} (panel id to file
     * line) matches no active organism.
     */
    void assignPanelDefaults(Map<String, Integer> lines, String actorId);

    MicroReferenceAdminPageForm<MicroPatientOriginAdminForm> getPatientOrigins(MicroReferenceAdminQueryForm query);

    List<MicroReferenceOptionForm> getOptions(String resource);
}
