package org.openelisglobal.typeofsample;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;
import org.springframework.beans.factory.annotation.Autowired;

public class TypeOfSamplePanelServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    TypeOfSamplePanelService panelService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/typeofsamplepanel.xml");
    }

    @Test
    public void getData_shouldCopyPropertiesFromDatabase() {
        TypeOfSamplePanel panel = new TypeOfSamplePanel();
        panel.setId("1");

        panelService.getData(panel);

        Assert.assertEquals("3", panel.getTypeOfSampleId());
        Assert.assertEquals("2", panel.getPanelId());
    }

    @Test
    public void getAllTypeOfSamplePanels_shouldReturnAllTypeOfSamplePanels() {
        List<TypeOfSamplePanel> panels = panelService.getAllTypeOfSamplePanels();

        Assert.assertEquals(4, panels.size());
    }

    @Test
    public void getPageOfTypeOfSamplePanel_shouldReturnPageOfTypeOfSamplePanel() {
        List<TypeOfSamplePanel> panelsPage = panelService.getPageOfTypeOfSamplePanel(1);

        int expectedPageSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        Assert.assertTrue(panelsPage.size() <= expectedPageSize);

        if (expectedPageSize >= 4) {
            Assert.assertTrue(panelsPage.stream().anyMatch(p -> p.getPanelId().equals("2")));
            Assert.assertTrue(panelsPage.stream().anyMatch(p -> p.getTypeOfSampleId().equals("4")));
        }
    }

    @Test
    public void getTotalTypeOfSamplePanelCount_shouldReturnTotalTypeOfSamplePanelCount() {

        Assert.assertEquals("4", panelService.getTotalTypeOfSamplePanelCount().toString());
    }

    @Test
    public void getTypeOfSamplePanelsForPanel_shouldReturnTypeOfSamplePanelsForPanel() {
        List<TypeOfSamplePanel> panels = panelService.getTypeOfSamplePanelsForPanel("4");

        Assert.assertEquals(1, panels.size());
        Assert.assertEquals("3", panels.get(0).getId());
    }

    @Test
    public void getTypeOfSamplePanelsForSampleType_shouldReturnTypeOfSamplePanelsForSampleType() {
        List<TypeOfSamplePanel> panels = panelService.getTypeOfSamplePanelsForSampleType("1");

        Assert.assertEquals(1, panels.size());
        Assert.assertEquals("4", panels.get(0).getId());
    }

}