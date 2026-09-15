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
import org.openelisglobal.menu.service.MenuService;
import org.openelisglobal.menu.util.MenuConfigurationLoader;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.util.MenuUtil;
import org.openelisglobal.menu.valueholder.Menu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MenuPresentationIntegrationTest extends BaseWebContextSensitiveTest {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private MenuService menus;

    @Before
    public void fixture() throws Exception {
        executeDataSetWithStateManagement("testdata/menu.xml");
        MenuUtil.forceRebuild();
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
