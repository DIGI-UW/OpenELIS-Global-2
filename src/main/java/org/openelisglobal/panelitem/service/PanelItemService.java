package org.openelisglobal.panelitem.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.security.access.prepost.PreAuthorize;

public interface PanelItemService extends BaseObjectService<PanelItem, String> {

    @PreAuthorize("hasAnyAuthority('PRIV_PANEL_VIEW','PRIV_CATALOGUE_VIEW')")
    void getData(PanelItem panelItem);

    @PreAuthorize("hasAnyAuthority('PRIV_PANEL_VIEW','PRIV_CATALOGUE_VIEW')")
    Integer getTotalPanelItemCount();

    @PreAuthorize("hasAnyAuthority('PRIV_PANEL_VIEW','PRIV_CATALOGUE_VIEW')")
    List<PanelItem> getPanelItemsForPanelAndItemList(String panelId, List<String> testList);

    @PreAuthorize("hasAnyAuthority('PRIV_PANEL_VIEW','PRIV_CATALOGUE_VIEW')")
    List<PanelItem> getPageOfPanelItems(int startingRecNo);

    @PreAuthorize("hasAnyAuthority('PRIV_PANEL_VIEW','PRIV_CATALOGUE_VIEW')")
    boolean getDuplicateSortOrderForPanel(PanelItem panelItem);

    @PreAuthorize("hasAnyAuthority('PRIV_PANEL_VIEW','PRIV_CATALOGUE_VIEW')")
    List<PanelItem> getPanelItemByTestId(String id);

    @PreAuthorize("hasAnyAuthority('PRIV_PANEL_VIEW','PRIV_CATALOGUE_VIEW')")
    List<PanelItem> getAllPanelItems();

    @PreAuthorize("hasAnyAuthority('PRIV_PANEL_VIEW','PRIV_CATALOGUE_VIEW')")
    List<PanelItem> getPanelItems(String filter);

    @PreAuthorize("hasAnyAuthority('PRIV_PANEL_VIEW','PRIV_CATALOGUE_VIEW')")
    List<PanelItem> getPanelItemsForPanel(String panelId);

    @PreAuthorize("hasAuthority('PRIV_PANEL_MANAGE')")
    void updatePanelItems(List<PanelItem> panelItems, Panel panel, boolean updatePanel, String currentUser,
            List<Test> newTests);

    @PreAuthorize("hasAnyAuthority('PRIV_PANEL_VIEW','PRIV_CATALOGUE_VIEW')")
    boolean duplicatePanelItemExists(PanelItem panelItem) throws LIMSRuntimeException;

    /**
     * OGC-949 M9: reconcile which panels a test belongs to, in one transaction.
     * {@code positionByPanelId} maps each desired panel id to this test's 1-based
     * position within it; memberships not in the map are removed. Only this test's
     * position is written (the editor doesn't renumber siblings — full panel
     * renumbering stays in Panel Management).
     */
    @PreAuthorize("hasAuthority('PRIV_PANEL_MANAGE')")
    void setMembershipsForTest(Test test, Map<String, Integer> positionByPanelId, String sysUserId);

    /**
     * OGC-224 — the panel-side mirror of {@link #setMembershipsForTest}: reconcile
     * a panel's ordered member tests in one transaction. {@code positionByTestId}
     * maps each desired test id to its 1-based position (panel_item.sort_order —
     * the same field the test-side Panels section edits: one model, two views);
     * member tests not in the map are removed.
     */
    @PreAuthorize("hasAuthority('PRIV_PANEL_MANAGE')")
    void setMembershipsForPanel(Panel panel, Map<String, Integer> positionByTestId, String sysUserId);
}
