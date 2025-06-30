package org.openelisglobal.menu.rest.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.valueholder.Menu;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class MenuRestControllerTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/menu.xml");
    }

    @Test
    public void getMenuTree_shouldReturnMenuTree() throws Exception {
        MvcResult urlResult = super.mockMvc.perform(get("/rest/menu").accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        String result = urlResult.getResponse().getContentAsString();

        ObjectMapper objectMapper = new ObjectMapper();
        List<MenuItem> menuMap = objectMapper.readValue(result, new TypeReference<List<MenuItem>>() {
        });
        assertNotNull("Menu list should not be null", menuMap);
        assertFalse("Menu list should not be empty", menuMap.isEmpty());

        assertEquals("testElement1", menuMap.get(0).getMenu().getElementId());
        assertTrue(menuMap.get(0).getMenu().getIsActive());

        long activeCount = menuMap.stream().filter(m -> m.getMenu().getIsActive()).count();
        assertEquals("Expected number of active menus", 6, activeCount);

    }

    @Test
    public void getMenuById_shouldReturmMenuItemGivenId() throws Exception {
        MvcResult urlResult = super.mockMvc.perform(get("/rest/menu/testElement1")
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        String result = urlResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        MenuItem menuWrapper = objectMapper.readValue(result, MenuItem.class);

        // Assertions
        assertNotNull("Menu should not be null", menuWrapper);
        assertNotNull("Menu object should not be null", menuWrapper.getMenu());

        assertEquals("testElement1", menuWrapper.getMenu().getElementId());
        assertEquals(1, menuWrapper.getMenu().getPresentationOrder());
        assertFalse("Menu should not open in new window", menuWrapper.getMenu().isOpenInNewWindow());
        assertTrue("Menu should be active", menuWrapper.getMenu().getIsActive());
        assertFalse("Menu should not be hidden in old UI", menuWrapper.getMenu().isHideInOldUI());

        // Assert childMenus is empty
        assertNotNull("Child menus list should not be null", menuWrapper.getChildMenus());
        assertTrue("Child menus should be empty", menuWrapper.getChildMenus().isEmpty());

    }

    @Test
    public void postMenuTree_shouldAddMenuList() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        Menu menu1 = new Menu();
        menu1.setElementId("elementTest20");
        menu1.setIsActive(true);
        menu1.setPresentationOrder(20);

        Menu menu2 = new Menu();
        menu2.setElementId("elementTest21");
        menu2.setIsActive(false);
        menu2.setPresentationOrder(21);

        MenuItem item1 = new MenuItem();
        item1.setMenu(menu1);
        MenuItem item2 = new MenuItem();
        item2.setMenu(menu2);

        List<MenuItem> menuItems = new ArrayList<>();
        menuItems.add(item1);
        menuItems.add(item2);

        String requestBody = objectMapper.writeValueAsString(menuItems);

        MvcResult postUrl = super.mockMvc.perform(post("/rest/menu").content(requestBody)
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        String result = postUrl.getResponse().getContentAsString();

        List<MenuItem> responseItems = objectMapper.readValue(result, new TypeReference<List<MenuItem>>() {
        });

        assertNotNull("Response should not be null", responseItems);
        assertEquals("Expected two menu items", 2, responseItems.size());

        assertEquals("elementTest20", responseItems.get(0).getMenu().getElementId());
        assertEquals(20, responseItems.get(0).getMenu().getPresentationOrder());
        assertTrue("Menu should be active", responseItems.get(0).getMenu().getIsActive());

        assertEquals("elementTest21", responseItems.get(1).getMenu().getElementId());
        assertEquals(21, responseItems.get(1).getMenu().getPresentationOrder());
        assertFalse("Menu should not be active", responseItems.get(1).getMenu().getIsActive());
    }

    @Test
    public void postMenuItem_shouldPostMenuItemByElementId() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Menu menu1 = new Menu();
        menu1.setElementId("elementTest20");
        menu1.setIsActive(true);
        menu1.setPresentationOrder(20);

        MenuItem item1 = new MenuItem();
        item1.setMenu(menu1);
        String requestBody = objectMapper.writeValueAsString(item1);

        MvcResult postUrl = super.mockMvc.perform(post("/rest/menu/elementTest20").content(requestBody)
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        String result = postUrl.getResponse().getContentAsString();
        MenuItem menuWrapper = objectMapper.readValue(result, MenuItem.class);

        assertNotNull("MenuItem response should not be null", menuWrapper);
        assertNotNull("Menu object should not be null", menuWrapper.getMenu());
        assertEquals("elementTest20", menuWrapper.getMenu().getElementId());
        assertTrue("Menu should be active", menuWrapper.getMenu().getIsActive());
        assertEquals(20, menuWrapper.getMenu().getPresentationOrder());
    }

}