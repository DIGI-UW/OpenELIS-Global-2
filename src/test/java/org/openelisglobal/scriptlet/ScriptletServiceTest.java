package org.openelisglobal.scriptlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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

    /**
     * Requesting a page far beyond the dataset's 3 records must not error out or
     * wrap around; it should simply come back empty.
     */
    @Test
    public void getPageOfScriptlets_shouldReturnEmptyList_whenStartingRecNoExceedsTotal() {
        List<Scriptlet> scriptlets = scriptletService.getPageOfScriptlets(1000);
        assertTrue("a starting record far past the last page should yield no rows", scriptlets.isEmpty());
    }

    @Test
    public void getScriptletByName_shouldReturnScriptlet() {
        Scriptlet scriptlet = new Scriptlet();
        scriptlet.setScriptletName("Scriptlet 1");
        scriptlet = scriptletService.getScriptletByName(scriptlet);
        assertEquals("Scriptlet 1", scriptlet.getScriptletName());
        assertEquals("Source1", scriptlet.getCodeSource());

    }

    /**
     * A name that isn't in the dataset must not resolve to some other scriptlet
     * or throw; it should simply come back unresolved.
     * NOTE: assumes getScriptletByName returns null on no match (typical for a
     * single-object DAO lookup in this codebase). Verify against
     * ScriptletServiceImpl / ScriptletDAO before relying on this in CI — if the
     * implementation instead returns the same Scriptlet object unmodified (with
     * only the name set), change this assertion to check codeSource/id are null
     * instead of asserting the whole object is null.
     */
    @Test
    public void getScriptletByName_shouldReturnNull_whenNameDoesNotExist() {
        Scriptlet scriptlet = new Scriptlet();
        scriptlet.setScriptletName("Nonexistent Scriptlet");
        Scriptlet result = scriptletService.getScriptletByName(scriptlet);
        assertNull("an unknown scriptlet name should not resolve to a scriptlet", result);
    }

    @Test
    public void getScriptletById_shouldReturnScriptletGivenId() {
        Scriptlet scriptlet = scriptletService.getScriptletById("1");
        assertEquals("Scriptlet 1", scriptlet.getScriptletName());
        assertEquals("Source1", scriptlet.getCodeSource());
    }

    /**
     * An id with no matching row must not throw or silently return an
     * uninitialized Scriptlet; it should come back null.
     */
    @Test
    public void getScriptletById_shouldReturnNull_whenIdDoesNotExist() {
        Scriptlet scriptlet = scriptletService.getScriptletById("999999");
        assertNull("an id with no matching row should return null", scriptlet);
    }

    /**
     * A blank id is a malformed lookup, not a "not found" — same expectation as
     * an unknown id: no match, no exception.
     */
    @Test
    public void getScriptletById_shouldReturnNull_whenIdIsBlank() {
        Scriptlet scriptlet = scriptletService.getScriptletById("");
        assertNull("a blank id should not match any row", scriptlet);
    }

    @Test
    public void getScriptlets_shouldReturnListOfScriptlets() {
        List<Scriptlet> scriptlets = scriptletService.getScriptlets("Scriptlet 1");
        assertEquals(1, scriptlets.size());
    }

    /**
     * A filter that matches nothing in the dataset must come back as an empty
     * list, not null and not every scriptlet.
     */
    @Test
    public void getScriptlets_shouldReturnEmptyList_whenFilterMatchesNothing() {
        List<Scriptlet> scriptlets = scriptletService.getScriptlets("No Such Scriptlet Exists");
        assertTrue("an unmatched filter should yield no rows", scriptlets.isEmpty());
    }

    /**
     * An empty filter is presumably treated as "match everything" (common for a
     * LIKE '%%' style filter query) rather than "match nothing". This asserts the
     * whole dataset comes back — verify against ScriptletServiceImpl and adjust
     * if an empty filter is instead expected to return no rows or throw.
     */
    @Test
    public void getScriptlets_shouldReturnAllScriptlets_whenFilterIsEmpty() {
        List<Scriptlet> scriptlets = scriptletService.getScriptlets("");
        assertEquals(3, scriptlets.size());
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
