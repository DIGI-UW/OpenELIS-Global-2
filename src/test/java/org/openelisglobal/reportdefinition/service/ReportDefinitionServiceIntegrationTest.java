package org.openelisglobal.reportdefinition.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reportdefinition.valueholder.ReportDefinition;
import org.springframework.beans.factory.annotation.Autowired;

public class ReportDefinitionServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReportDefinitionService reportDefinitionService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/user-role.xml");
        executeDataSetWithStateManagement("testdata/report-definition-test-data.xml");
    }

    @Test
    public void getAll_validCall_returnsNonNullList() {
        List<ReportDefinition> definitions = reportDefinitionService.getAll();
        assertNotNull(definitions);
    }

    @Test
    public void getAll_validCall_returnsAllDefinitions() {
        List<ReportDefinition> definitions = reportDefinitionService.getAll();
        assertNotNull(definitions);
        assertTrue(definitions.size() >= 2);
    }

    @Test
    public void getActiveDefinitions_validCall_returnsOnlyActive() {
        List<ReportDefinition> active = reportDefinitionService.getActiveDefinitions();
        assertNotNull(active);
        assertTrue(active.size() >= 1);
        for (ReportDefinition def : active) {
            assertTrue(def.getIsActive());
        }
    }

    @Test
    public void getDefinitionsByCategory_validCategory_returnsMatching() {
        List<ReportDefinition> defs = reportDefinitionService.getDefinitionsByCategory("LAB");
        assertNotNull(defs);
        assertTrue(defs.size() >= 1);
        for (ReportDefinition def : defs) {
            assertEquals("LAB", def.getCategory());
        }
    }

    @Test
    public void getDefinitionsByCategory_invalidCategory_returnsEmpty() {
        List<ReportDefinition> defs = reportDefinitionService.getDefinitionsByCategory("NONEXISTENT");
        assertNotNull(defs);
        assertTrue(defs.isEmpty());
    }

    @Test
    public void get_validId_returnsCorrectDefinition() {
        ReportDefinition def = reportDefinitionService.get("RPT-001");
        assertNotNull(def);
        assertEquals("RPT-001", def.getId());
        assertNotNull(def.getName());
        assertTrue(def.getIsActive());
    }

    @Test
    public void get_inactiveId_returnsInactiveDefinition() {
        ReportDefinition def = reportDefinitionService.get("RPT-002");
        assertNotNull(def);
        assertFalse(def.getIsActive());
    }
}
