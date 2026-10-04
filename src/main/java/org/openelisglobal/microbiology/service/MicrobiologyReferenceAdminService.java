package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.MicroAntibioticAdminForm;
import org.openelisglobal.microbiology.form.MicroAstPanelAdminForm;
import org.openelisglobal.microbiology.form.MicroCultureSetupAdminForm;
import org.openelisglobal.microbiology.form.MicroOrganismAdminForm;
import org.openelisglobal.microbiology.form.MicroPatientOriginAdminForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminPageForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminQueryForm;
import org.openelisglobal.microbiology.form.MicroReferenceOptionForm;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicrobiologyReferenceAdminService {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroReferenceAdminPageForm<MicroOrganismAdminForm> getOrganisms(MicroReferenceAdminQueryForm query);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroOrganismAdminForm getOrganism(String id);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroOrganismAdminForm saveOrganism(String id, MicroOrganismAdminForm request, String actorId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroOrganismAdminForm setOrganismActive(String id, boolean active, String actorId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroReferenceAdminPageForm<MicroAntibioticAdminForm> getAntibiotics(MicroReferenceAdminQueryForm query);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroAntibioticAdminForm getAntibiotic(String id);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroAntibioticAdminForm saveAntibiotic(String id, MicroAntibioticAdminForm request, String actorId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroAntibioticAdminForm setAntibioticActive(String id, boolean active, String actorId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroReferenceAdminPageForm<MicroAstPanelAdminForm> getAstPanels(MicroReferenceAdminQueryForm query);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroAstPanelAdminForm getAstPanel(String id);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroAstPanelAdminForm createPanel(MicroAstPanelAdminForm request, String actorId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroAstPanelAdminForm publishPanelVersion(String currentPanelId, MicroAstPanelAdminForm request, String actorId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroReferenceAdminPageForm<MicroCultureSetupAdminForm> getCultureSetups(MicroReferenceAdminQueryForm query);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroReferenceAdminPageForm<MicroPatientOriginAdminForm> getPatientOrigins(MicroReferenceAdminQueryForm query);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroCultureSetupAdminForm getCultureSetup(String id);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroCultureSetupAdminForm saveCultureSetup(String id, MicroCultureSetupAdminForm request, String actorId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<MicroReferenceOptionForm> getOptions(String resource);
}
