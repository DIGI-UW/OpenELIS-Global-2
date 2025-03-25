package org.openelisglobal.scriptlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.scriptlet.service.ScriptletService;
import org.openelisglobal.scriptlet.valueholder.Scriptlet;
import org.springframework.beans.factory.annotation.Autowired;

public class ScriptletServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ScriptletService scriptletService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/scriptlet.xml");
    }

    // @Test
    public void testDataBase() {
        List<Scriptlet> scriptlets = scriptletService.getAll();
        scriptlets.forEach(scriptlet -> {
            System.out.print(scriptlet.getScriptletName() + " ");
        });
    }

    @Test
    public void testGetData() {
        Scriptlet scriptlet = scriptletService.get("1");
        scriptletService.getData(scriptlet);
        assertEquals("Scriptlet 1", scriptlet.getScriptletName());
        assertEquals("Source1", scriptlet.getCodeSource());

    }

    @Test
    public void getPageOfScriptlets_shouldReturnListOfScriptlets() {
        List<Scriptlet> scriptlets = scriptletService.getPageOfScriptlets(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(scriptlets.size() <= expectedPages);
    }

    @Test
    public void getScriptletByName_shouldReturnScriptlet() {
        Scriptlet scriptlet = new Scriptlet();
        scriptlet.setScriptletName("Scriptlet 1");
        scriptlet = scriptletService.getScriptletByName(scriptlet);
        assertEquals("Scriptlet 1", scriptlet.getScriptletName());
        assertEquals("Source1", scriptlet.getCodeSource());

    }

    @Test
    public void getScriptletById_shouldReturnScriptletGivenId() {
        Scriptlet scriptlet = scriptletService.getScriptletById("1");
        assertEquals("Scriptlet 1", scriptlet.getScriptletName());
        assertEquals("Source1", scriptlet.getCodeSource());
    }

    @Test
    public void getScriptlets_shouldReturnListOfScriptlets() {
        List<Scriptlet> scriptlets = scriptletService.getScriptlets("Scriptlet 1");
        assertEquals(1, scriptlets.size());
    }

    @Test
    public void getTotalScriptletCount_shouldReturnTotalNumberOfScriptlets() {
        int totalScriptletCount = scriptletService.getTotalScriptletCount();
        assertEquals(3, totalScriptletCount);
    }

    @Test
    public void getAllScriptlets_shouldReturnListOfScriptlets() {
        List<Scriptlet> scriptlets = scriptletService.getAll();
        assertEquals(3, scriptlets.size());
    }
}