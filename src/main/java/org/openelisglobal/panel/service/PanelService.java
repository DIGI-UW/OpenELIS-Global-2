package org.openelisglobal.panel.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.valueholder.Panel;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_PANEL_VIEW')")
public interface PanelService extends BaseObjectService<Panel, String> {

    void getData(Panel panel);

    String getIdForPanelName(String name);

    String getDescriptionForPanelId(String id);

    String getNameForPanelId(String panelId);

    List<Panel> getAllActivePanels();

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
