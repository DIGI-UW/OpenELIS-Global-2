package org.openelisglobal.panelItem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.springframework.beans.factory.annotation.Autowired;

public class PanelItemServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PanelItemService panelItemService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/panel-item.xml");
    }

    @Test
    public void getData() {
        PanelItem panelItem = panelItemService.get("1");
        panelItemService.getData(panelItem);
        assertEquals("1", panelItem.getId());
        assertEquals("1", panelItem.getPanel().getId());
    }

    @Test
    public void getTotalPanelItemCount() {
        List<PanelItem> panelItems = panelItemService.getAll();
        int totalCount = panelItemService.getTotalPanelItemCount();
        assertEquals(panelItems.size(), totalCount);
    }

    @Test
    public void getPageOfPanelItems() {
        List<PanelItem> panelItems = panelItemService.getPageOfPanelItems(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(panelItems.size() <= expectedPages);

    }

    @Test
    public void getAllPanelItems() {
        List<PanelItem> panelItems = panelItemService.getAllPanelItems();
        assertEquals(2, panelItems.size());
        assertEquals("1", panelItems.get(0).getId());
        assertEquals("2", panelItems.get(1).getId());
    }

    @Test
    public void getPanelItemsByTestId() {
        List<PanelItem> panelItems = panelItemService.getPanelItemByTestId("1");
        assertEquals(2, panelItems.size());
        assertEquals("1", panelItems.get(0).getId());
        assertEquals("2", panelItems.get(1).getId());
    }

    @Test
    public void getPanelItems() {
        List<PanelItem> panelItems = panelItemService.getPanelItems("T");
        assertEquals(1, panelItems.size());
        assertEquals("1", panelItems.get(0).getId());
    }

    @Test
    public void getPanelItemsForPanel() {
        List<PanelItem> panelItems = panelItemService.getPanelItemsForPanel("1");
        assertEquals(1, panelItems.size());
        assertEquals("1", panelItems.get(0).getId());
    }

    @Test
    public void getPanelItemsForPanelAndItemList() {
        List<PanelItem> panelItems = panelItemService.getPanelItemsForPanelAndItemList("1", List.of(1));
        assertEquals(1, panelItems.size());
        assertEquals("1", panelItems.get(0).getId());
    }

}
