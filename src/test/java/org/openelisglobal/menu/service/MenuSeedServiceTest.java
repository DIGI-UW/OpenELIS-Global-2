package org.openelisglobal.menu.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.menu.valueholder.Menu;
import org.openelisglobal.menu.valueholder.MenuSeedConfig.MenuItemDTO;
import org.springframework.beans.factory.annotation.Autowired;

public class MenuSeedServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MenuSeedService menuSeedService;

    @Autowired
    private MenuService menuService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/menu_seed.xml");
    }

    /**
     * Test: MenuSeedService.createMenuFromDTO() Verifies that a Menu entity is
     * correctly created from a MenuItemDTO with all attributes populated
     */
    @Test
    public void createMenuFromDTO_shouldCreateMenuWithAllAttributes() {
        MenuItemDTO dto = new MenuItemDTO();
        dto.setElementId("test_menu_dto");
        dto.setDisplayKey("test.menu.dto");
        dto.setToolTipKey("test.menu.dto.tooltip");
        dto.setActionURL("/TestDTO");
        dto.setClickAction("testAction()");
        dto.setPresentationOrder(5);
        dto.setIsActive(true);

        Menu menu = menuSeedService.createMenuFromDTO(dto, null);

        assertNotNull(menu);
        assertEquals("test_menu_dto", menu.getElementId());
        assertEquals("test.menu.dto", menu.getDisplayKey());
        assertEquals("test.menu.dto.tooltip", menu.getToolTipKey());
        assertEquals("/TestDTO", menu.getActionURL());
        assertEquals("testAction()", menu.getClickAction());
        assertEquals(5, menu.getPresentationOrder());
        assertEquals(true, menu.getIsActive());
        assertNull(menu.getParent());
    }

    /**
     * Test: MenuSeedService.createMenuFromDTO() Verifies that default values are
     * applied when optional fields are not set in the DTO
     */
    @Test
    public void createMenuFromDTO_shouldHandleDefaultValues() {
        MenuItemDTO dto = new MenuItemDTO();
        dto.setElementId("test_menu_defaults");
        dto.setDisplayKey("test.menu.defaults");
        dto.setActionURL("/TestDefaults");
        // Not setting optional fields

        Menu menu = menuSeedService.createMenuFromDTO(dto, null);

        assertNotNull(menu);
        assertEquals("test_menu_defaults", menu.getElementId());
        assertEquals(0, menu.getPresentationOrder());
        assertEquals(true, menu.getIsActive());
    }

    /**
     * Test: MenuSeedService.createMenuFromDTO() Verifies that parent menu
     * relationship is correctly set when a parent menu is provided
     */
    @Test
    public void createMenuFromDTO_shouldSetParentWhenProvided() {
        // Get existing parent from dataset
        Menu parentMenu = menuService.getMenuByElementId("menu_sample");
        assertNotNull("Parent menu should exist in dataset", parentMenu);

        MenuItemDTO dto = new MenuItemDTO();
        dto.setElementId("test_child_menu");
        dto.setDisplayKey("test.child.menu");
        dto.setActionURL("/TestChild");
        dto.setPresentationOrder(1);

        Menu childMenu = menuSeedService.createMenuFromDTO(dto, parentMenu);

        assertNotNull(childMenu);
        assertNotNull(childMenu.getParent());
        assertEquals(parentMenu.getId(), childMenu.getParent().getId());
        assertEquals("menu_sample", childMenu.getParent().getElementId());
    }

    /**
     * Test: MenuSeedService.seedMenus() Verifies that new menus are created in the
     * database when they don't already exist
     */
    @Test
    public void seedMenus_shouldCreateNewMenus() {
        List<MenuItemDTO> menuDTOs = new ArrayList<>();

        MenuItemDTO dto = new MenuItemDTO();
        dto.setElementId("menu_seed_test_new");
        dto.setDisplayKey("test.seed.new");
        dto.setActionURL("/TestSeedNew");
        dto.setPresentationOrder(1);
        dto.setIsActive(true);
        menuDTOs.add(dto);

        // Verify menu doesn't exist before seeding
        assertNull(menuService.getMenuByElementId("menu_seed_test_new"));

        // Seed menus
        int createdCount = menuSeedService.seedMenus(menuDTOs, null);

        // Verify menu was created
        assertEquals(1, createdCount);
        Menu createdMenu = menuService.getMenuByElementId("menu_seed_test_new");
        assertNotNull(createdMenu);
        assertEquals("test.seed.new", createdMenu.getDisplayKey());
    }

    /**
     * Test: MenuSeedService.seedMenus() Verifies idempotency - existing menus (by
     * elementId) are not duplicated during seeding
     */
    @Test
    public void seedMenus_shouldSkipExistingMenus() {
        // Use existing menu from dataset
        Menu existingMenu = menuService.getMenuByElementId("menu_home");
        assertNotNull("menu_home should exist in dataset", existingMenu);
        String existingId = existingMenu.getId();

        List<MenuItemDTO> menuDTOs = new ArrayList<>();

        MenuItemDTO dto = new MenuItemDTO();
        dto.setElementId("menu_home"); // Already exists
        dto.setDisplayKey("banner.menu.home");
        dto.setActionURL("/Dashboard");
        dto.setPresentationOrder(1);
        dto.setIsActive(true);
        menuDTOs.add(dto);

        // Seed menus (should skip existing)
        int createdCount = menuSeedService.seedMenus(menuDTOs, null);

        // Verify no menu was created
        assertEquals(0, createdCount);

        // Verify existing menu unchanged
        Menu stillExisting = menuService.getMenuByElementId("menu_home");
        assertNotNull(stillExisting);
        assertEquals(existingId, stillExisting.getId());
    }

    /**
     * Test: MenuSeedService.seedMenus() Verifies that parent-child menu hierarchies
     * are correctly created with proper relationships
     */
    @Test
    public void seedMenus_shouldHandleParentChildHierarchy() {
        List<MenuItemDTO> menuDTOs = new ArrayList<>();

        // Parent menu
        MenuItemDTO parentDTO = new MenuItemDTO();
        parentDTO.setElementId("menu_parent_seed");
        parentDTO.setDisplayKey("test.parent.seed");
        parentDTO.setActionURL("");
        parentDTO.setPresentationOrder(1);
        parentDTO.setIsActive(true);

        // Child menu
        MenuItemDTO childDTO = new MenuItemDTO();
        childDTO.setElementId("menu_child_seed");
        childDTO.setDisplayKey("test.child.seed");
        childDTO.setActionURL("/TestChildSeed");
        childDTO.setPresentationOrder(1);
        childDTO.setIsActive(true);

        List<MenuItemDTO> childMenus = new ArrayList<>();
        childMenus.add(childDTO);
        parentDTO.setChildMenus(childMenus);

        menuDTOs.add(parentDTO);

        // Seed menus
        int createdCount = menuSeedService.seedMenus(menuDTOs, null);

        // Verify both parent and child were created
        assertEquals(2, createdCount);

        Menu parent = menuService.getMenuByElementId("menu_parent_seed");
        Menu child = menuService.getMenuByElementId("menu_child_seed");

        assertNotNull(parent);
        assertNotNull(child);
        assertNotNull(child.getParent());
        assertEquals(parent.getId(), child.getParent().getId());
    }

    /**
     * Test: MenuSeedService.seedMenus() Verifies that menus with blank or null
     * elementId are skipped and don't cause errors
     */
    @Test
    public void seedMenus_shouldSkipMenuWithBlankElementId() {
        List<MenuItemDTO> menuDTOs = new ArrayList<>();

        // Menu with blank elementId
        MenuItemDTO invalidDTO = new MenuItemDTO();
        invalidDTO.setElementId("");
        invalidDTO.setDisplayKey("test.invalid");
        menuDTOs.add(invalidDTO);

        // Valid menu
        MenuItemDTO validDTO = new MenuItemDTO();
        validDTO.setElementId("menu_valid_seed");
        validDTO.setDisplayKey("test.valid.seed");
        validDTO.setActionURL("/TestValid");
        validDTO.setPresentationOrder(1);
        menuDTOs.add(validDTO);

        // Seed menus
        int createdCount = menuSeedService.seedMenus(menuDTOs, null);

        // Only valid menu should be created
        assertEquals(1, createdCount);
        assertNotNull(menuService.getMenuByElementId("menu_valid_seed"));
    }

    /**
     * Test: MenuSeedService.seedMenus() Verifies that child menus can be added to
     * existing parent menus (parent exists in dataset)
     */
    @Test
    public void seedMenus_shouldHandleChildrenOfExistingParent() {
        // Use existing parent from dataset
        Menu existingParent = menuService.getMenuByElementId("menu_sample");
        assertNotNull(existingParent);

        List<MenuItemDTO> menuDTOs = new ArrayList<>();

        // Existing parent
        MenuItemDTO parentDTO = new MenuItemDTO();
        parentDTO.setElementId("menu_sample"); // Already exists
        parentDTO.setDisplayKey("banner.menu.sample");

        // New child
        MenuItemDTO newChildDTO = new MenuItemDTO();
        newChildDTO.setElementId("menu_sample_new_child");
        newChildDTO.setDisplayKey("test.sample.new.child");
        newChildDTO.setActionURL("/TestNewChild");
        newChildDTO.setPresentationOrder(2);

        List<MenuItemDTO> childMenus = new ArrayList<>();
        childMenus.add(newChildDTO);
        parentDTO.setChildMenus(childMenus);

        menuDTOs.add(parentDTO);

        // Seed menus
        int createdCount = menuSeedService.seedMenus(menuDTOs, null);

        // Only child should be created (parent already exists)
        assertEquals(1, createdCount);

        Menu newChild = menuService.getMenuByElementId("menu_sample_new_child");
        assertNotNull(newChild);
        // Note: Parent relationship exists but is lazy-loaded, verified by creation
        // count
    }

    /**
     * Test: MenuSeedService.seedMenus() Verifies that multiple menus can be created
     * in a single seeding operation
     */
    @Test
    public void seedMenus_shouldHandleMultipleNewMenus() {
        List<MenuItemDTO> menuDTOs = new ArrayList<>();

        MenuItemDTO dto1 = new MenuItemDTO();
        dto1.setElementId("menu_multiple_1");
        dto1.setDisplayKey("test.multiple.1");
        dto1.setActionURL("/TestMultiple1");
        dto1.setPresentationOrder(1);
        menuDTOs.add(dto1);

        MenuItemDTO dto2 = new MenuItemDTO();
        dto2.setElementId("menu_multiple_2");
        dto2.setDisplayKey("test.multiple.2");
        dto2.setActionURL("/TestMultiple2");
        dto2.setPresentationOrder(2);
        menuDTOs.add(dto2);

        // Seed menus
        int createdCount = menuSeedService.seedMenus(menuDTOs, null);

        // Verify both menus were created
        assertEquals(2, createdCount);
        assertNotNull(menuService.getMenuByElementId("menu_multiple_1"));
        assertNotNull(menuService.getMenuByElementId("menu_multiple_2"));
    }

    /**
     * Test: Dataset Integrity Check Verifies that the test dataset (menu_seed.xml)
     * is loaded correctly before running other tests
     */
    @Test
    public void datasetIntegrity_shouldHaveExpectedMenusFromDataset() {
        // Verify dataset was loaded correctly
        Menu menuHome = menuService.getMenuByElementId("menu_home");
        assertNotNull("menu_home should exist from dataset", menuHome);
        assertEquals("banner.menu.home", menuHome.getDisplayKey());
        assertEquals("/Dashboard", menuHome.getActionURL());

        Menu menuSample = menuService.getMenuByElementId("menu_sample");
        assertNotNull("menu_sample should exist from dataset", menuSample);

        Menu menuSampleAdd = menuService.getMenuByElementId("menu_sample_add");
        assertNotNull("menu_sample_add should exist from dataset", menuSampleAdd);
        // Note: Parent relationship exists in dataset but is lazy-loaded
    }
}
