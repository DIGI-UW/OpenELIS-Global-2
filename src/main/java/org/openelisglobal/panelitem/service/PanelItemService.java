package org.openelisglobal.panelitem.service;

import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.security.access.prepost.PreAuthorize;

public interface PanelItemService extends BaseObjectService<PanelItem, String> {

    @PreAuthorize("hasAuthority('PRIV_PANEL_VIEW')")
    void getData(PanelItem panelItem);

    @PreAuthorize("hasAuthority('PRIV_PANEL_VIEW')")
    Integer getTotalPanelItemCount();

    @PreAuthorize("hasAuthority('PRIV_PANEL_VIEW')")
    List<PanelItem> getPanelItemsForPanelAndItemList(String panelId, List<Integer> testList);

    @PreAuthorize("hasAuthority('PRIV_PANEL_VIEW')")
    List<PanelItem> getPageOfPanelItems(int startingRecNo);

    @PreAuthorize("hasAuthority('PRIV_PANEL_VIEW')")
    boolean getDuplicateSortOrderForPanel(PanelItem panelItem);

    @PreAuthorize("hasAuthority('PRIV_PANEL_VIEW')")
    List<PanelItem> getPanelItemByTestId(String id);

    @PreAuthorize("hasAuthority('PRIV_PANEL_VIEW')")
    List<PanelItem> getAllPanelItems();

    @PreAuthorize("hasAuthority('PRIV_PANEL_VIEW')")
    List<PanelItem> getPanelItems(String filter);

    @PreAuthorize("hasAuthority('PRIV_PANEL_VIEW')")
    List<PanelItem> getPanelItemsForPanel(String panelId);

    @PreAuthorize("hasAuthority('PRIV_PANEL_MANAGE')")
    void updatePanelItems(List<PanelItem> panelItems, Panel panel, boolean updatePanel, String currentUser,
            List<Test> newTests);

    @PreAuthorize("hasAuthority('PRIV_PANEL_VIEW')")
    boolean duplicatePanelItemExists(PanelItem panelItem) throws LIMSRuntimeException;
}
