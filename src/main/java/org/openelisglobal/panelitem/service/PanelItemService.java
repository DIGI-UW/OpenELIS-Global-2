package org.openelisglobal.panelitem.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.test.valueholder.Test;

public interface PanelItemService extends BaseObjectService<PanelItem, String> {
    void getData(PanelItem panelItem);

    Integer getTotalPanelItemCount();

    List<PanelItem> getPanelItemsForPanelAndItemList(String panelId, List<String> testList);

    List<PanelItem> getPageOfPanelItems(int startingRecNo);

    boolean getDuplicateSortOrderForPanel(PanelItem panelItem);

    List<PanelItem> getPanelItemByTestId(String id);

    List<PanelItem> getAllPanelItems();

    List<PanelItem> getPanelItems(String filter);

    List<PanelItem> getPanelItemsForPanel(String panelId);

    void updatePanelItems(List<PanelItem> panelItems, Panel panel, boolean updatePanel, String currentUser,
            List<Test> newTests);

    boolean duplicatePanelItemExists(PanelItem panelItem) throws LIMSRuntimeException;

    /**
     * OGC-949 M9: reconcile which panels a test belongs to, in one transaction.
     * {@code positionByPanelId} maps each desired panel id to this test's 1-based
     * position within it; memberships not in the map are removed. Only this test's
     * position is written (the editor doesn't renumber siblings — full panel
     * renumbering stays in Panel Management).
     */
    void setMembershipsForTest(Test test, Map<String, Integer> positionByPanelId, String sysUserId);
}
