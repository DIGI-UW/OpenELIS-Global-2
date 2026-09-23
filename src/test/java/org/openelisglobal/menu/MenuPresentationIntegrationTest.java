package org.openelisglobal.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.audittrail.daoimpl.AuditTrailServiceImpl;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.menu.service.MenuService;
import org.openelisglobal.menu.util.MenuConfigurationLoader;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.util.MenuUtil;
import org.openelisglobal.menu.valueholder.Menu;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MenuPresentationIntegrationTest extends BaseWebContextSensitiveTest {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private MenuService menus;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Before
    public void fixture() throws Exception {
        executeDataSetWithStateManagement("testdata/menu.xml");
        MenuUtil.forceRebuild();
        // AppTestConfig mocks AuditTrailService for the whole test profile, so the
        // real diff-and-insert never runs unless it is swapped back in here. Same
        // approach as P0AuditEmitSmokeTest.
        AuditTrailServiceImpl realAuditTrail = new AuditTrailServiceImpl();
        ReflectionTestUtils.setField(realAuditTrail, "referenceTablesService", referenceTablesService);
        ReflectionTestUtils.setField(realAuditTrail, "historyService", historyService);
        Object menuServiceTarget = AopTestUtils.getUltimateTargetObject(menus);
        ReflectionTestUtils.setField(menuServiceTarget, "auditTrailService", realAuditTrail);
        cleanRowsInCurrentConnection(new String[] { "history" });
    }

    @After
    public void clearMenuCache() {
        MenuUtil.forceRebuild();
    }

    @Test
    public void administrativeSavePersistsPresentationAndKeepsExistingHierarchy() {
        Menu original = menus.getMenuByElementId("testElement2");
        original.setParent(menus.getMenuByElementId("testElement1"));
        original.setDisplayKey("instance.custom.menu");
        entityManager.flush();
        entityManager.clear();

        Menu edit = new Menu();
        edit.setElementId("testElement2");
        edit.setActionURL("/reports/custom-data-export");
        edit.setIsActive(true);
        edit.setPresentationStyle("section");
        edit.setIcon("reports");
        MenuItem request = new MenuItem();
        request.setMenu(edit);
        menus.save(request);
        entityManager.flush();
        entityManager.clear();

        Menu stored = menus.getMenuByElementId("testElement2");
        assertEquals("section", stored.getPresentationStyle());
        assertEquals("reports", stored.getIcon());
        assertEquals("testElement1", stored.getParent().getElementId());
        assertEquals("instance.custom.menu", stored.getDisplayKey());
        assertEquals(2, stored.getPresentationOrder());
        assertEquals("/reports/custom-data-export", stored.getActionURL());
        assertTrue(stored.getIsActive());
    }

    /**
     * Menu carries globally visible navigation configuration, so a presentation
     * edit must be attributable. The second half matters as much as the first:
     * saveHistory writes only when it finds a real diff, so re-saving identical
     * state must add nothing — otherwise every navigation rebuild, which re-saves
     * the whole tree, would bloat the trail.
     */
    @Test
    public void presentationEditIsAudited_andIdenticalResaveAddsNothing() {
        String menuId = menus.getMenuByElementId("testElement2").getId();
        String menuTable = referenceTablesService.getReferenceTableByName("menu").getId();
        entityManager.flush();
        entityManager.clear();

        savePresentation("wrench");
        assertEquals(1, historyService.getHistoryByRefIdAndRefTableId(menuId, menuTable).size());

        savePresentation("reports");
        List<History> rows = historyService.getHistoryByRefIdAndRefTableId(menuId, menuTable);
        assertEquals(2, rows.size());
        // getHistoryByRefIdAndRefTableId returns newest first.
        History latest = rows.get(0);
        assertEquals("U", latest.getActivity());
        assertTrue("audit entry should retain the icon that was replaced",
                new String(latest.getChanges()).contains("wrench"));

        // Identical state re-saved: no diff, so no third row.
        savePresentation("reports");
        assertEquals(2, historyService.getHistoryByRefIdAndRefTableId(menuId, menuTable).size());
    }

    private void savePresentation(String icon) {
        Menu edit = new Menu();
        edit.setElementId("testElement2");
        edit.setActionURL("/reports/custom-data-export");
        edit.setIsActive(true);
        edit.setPresentationStyle("section");
        edit.setIcon(icon);
        MenuItem request = new MenuItem();
        request.setMenu(edit);
        menus.save(request);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    public void removingAnInstanceOverlayRestoresPersistedPresentation() throws Exception {
        Menu baseline = menus.getMenuByElementId("testElement1");
        baseline.setPresentationStyle("section");
        baseline.setIcon("reports");
        entityManager.flush();
        entityManager.clear();
        Path profile = Files.createTempFile("menu-presentation-", ".json");
        try {
            Files.writeString(profile, """
                    {"menus":[{"elementId":"testElement1","icon":"settings","presentationStyle":""}]}
                    """);
            List<Menu> effective = new ArrayList<>(List.of(menus.getMenuByElementId("testElement1")));
            MenuConfigurationLoader.loadConfiguredMenus(profile.toFile(), effective);
            assertEquals("settings", effective.get(0).getIcon());
            assertEquals("", effective.get(0).getPresentationStyle());
            entityManager.flush();
            entityManager.clear();
            Menu restored = menus.getMenuByElementId("testElement1");
            assertEquals("reports", restored.getIcon());
            assertEquals("section", restored.getPresentationStyle());
        } finally {
            Files.deleteIfExists(profile);
        }
    }

    @Test
    public void olderAdministrativeRequestsDoNotErasePresentation() {
        Menu baseline = menus.getMenuByElementId("testElement3");
        baseline.setIcon("patient");
        baseline.setPresentationStyle("section");
        entityManager.flush();
        entityManager.clear();
        Menu edit = new Menu();
        edit.setElementId("testElement3");
        edit.setIsActive(false);
        MenuItem request = new MenuItem();
        request.setMenu(edit);
        menus.save(request);
        entityManager.flush();
        entityManager.clear();
        Menu stored = menus.getMenuByElementId("testElement3");
        assertEquals("patient", stored.getIcon());
        assertEquals("section", stored.getPresentationStyle());
        assertNull(stored.getActionURL());
    }

    @Test
    public void savingAnOverlaidMenuNeverCopiesControlledValuesIntoDatabaseDefaults() throws Exception {
        Menu baseline = menus.getMenuByElementId("testElement1");
        baseline.setIcon("patient");
        baseline.setActionURL("/OriginalInstanceDestination");
        baseline.setIsActive(false);
        entityManager.flush();
        entityManager.clear();
        Path profile = Files.createTempFile("menu-controlled-", ".json");
        String property = "org.openelisglobal.menu.configuration.file";
        String previous = System.getProperty(property);
        try {
            Files.writeString(profile, """
                    {"menus":[{"elementId":"testElement1","icon":"reports",
                    "actionURL":"/reports/custom-data-export","isActive":true}]}
                    """);
            System.setProperty(property, profile.toString());
            MenuUtil.forceRebuild();
            Menu effective = MenuUtil.getUnfilteredMenuTree().stream()
                    .filter(item -> "testElement1".equals(item.getMenu().getElementId())).findFirst().orElseThrow()
                    .getMenu();
            assertTrue(effective.getConfigurationFields().containsAll(List.of("icon", "isActive", "actionURL")));
            assertEquals("reports", effective.getIcon());
            Menu edit = new Menu();
            edit.setElementId("testElement1");
            edit.setIcon("reports");
            edit.setActionURL("/reports/custom-data-export");
            edit.setIsActive(true);
            edit.setPresentationStyle("section");
            MenuItem request = new MenuItem();
            request.setMenu(edit);
            menus.save(request);
            entityManager.flush();
            entityManager.clear();
            Menu stored = menus.getMenuByElementId("testElement1");
            assertEquals("patient", stored.getIcon());
            assertEquals("/OriginalInstanceDestination", stored.getActionURL());
            assertEquals(false, stored.getIsActive());
            assertEquals("section", stored.getPresentationStyle());
        } finally {
            if (previous == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, previous);
            }
            MenuUtil.forceRebuild();
            Files.deleteIfExists(profile);
        }
    }
}
