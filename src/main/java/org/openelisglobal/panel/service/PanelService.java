package org.openelisglobal.panel.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.valueholder.Panel;

public interface PanelService extends BaseObjectService<Panel, String> {

    void getData(Panel panel);

    String getIdForPanelName(String name);

    String getDescriptionForPanelId(String id);

    String getNameForPanelId(String panelId);

    List<Panel> getAllActivePanels();

    /**
     * V-03 (FR-V03-PNL-001/004) — active panels filtered by workflow domain via the
     * existing {@code TypeOfSamplePanel} junction and {@code TypeOfSample
     * .domain}. {@code panelDomain} accepts the friendly name (CLINICAL /
     * ENVIRONMENTAL / VECTOR / ANIMAL) or the single-letter code (H/E/V/A). Pass
     * null/blank to fall back to {@link #getAllActivePanels()}.
     */
    List<Panel> getAllActivePanelsByDomain(String panelDomain);

    /**
     * V-03 (FR-V03-PNL-004) — VECTOR-domain active panels with the supplied
     * organism group (a {@code type_of_sample.id} where {@code domain='V'}) ranked
     * first ("Suggested" label in the UI). Pass null/blank for the unsorted
     * VECTOR-only list.
     */
    List<Panel> getActiveVectorPanelsForOrganismGroup(String vectorOrganismGroupId);

    Integer getTotalPanelCount();

    List<Panel> getActivePanels(String filter);

    Panel getPanelByName(String panelName);

    Panel getPanelByName(Panel panel);

    Panel getPanelById(String id);

    List<Panel> getPageOfPanels(int startingRecNo);

    List<Panel> getAllPanels();

    Localization getLocalizationForPanel(String id);

    Panel getPanelByLoincCode(String loincCode);

}
