package org.openelisglobal.menu.rest.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.valueholder.Menu;
import org.springframework.http.MediaType;

public class MenuRestControllerTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/menu.xml");
    }

    @Test
    public void getMenuTree_shouldReturnMenuTree() throws Exception {
        super.mockMvc.perform(get("/rest/menu")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].menu.elementId").value("testElement1"))
                .andExpect(jsonPath("$[0].menu.isActive").value(true));
    }

    @Test
    public void getMenuById_shouldReturmMenuItemGivenId() throws Exception {
        super.mockMvc.perform(get("/rest/menu/testElement1")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menu.elementId").value("testElement1"))
                .andExpect(jsonPath("$.menu.presentationOrder").value(1))
                .andExpect(jsonPath("$.menu.openInNewWindow").value(false))
                .andExpect(jsonPath("$.menu.isActive").value(true))
                .andExpect(jsonPath("$.menu.hideInOldUI").value(false))
                .andExpect(jsonPath("$.childMenus.length()").value(0));
    }

    @Test
    public void getAdminMenuById_shouldReturnInactiveMenuItemGivenId() throws Exception {
        super.mockMvc.perform(get("/rest/admin/menu/testElement2")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menu.elementId").value("testElement2"))
                .andExpect(jsonPath("$.menu.isActive").value(false));
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

        super.mockMvc.perform(post("/rest/menu")
                .content(requestBody)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].menu.elementId").value("elementTest20"))
                .andExpect(jsonPath("$[0].menu.presentationOrder").value(20))
                .andExpect(jsonPath("$[0].menu.isActive").value(true))
                .andExpect(jsonPath("$[1].menu.elementId").value("elementTest21"))
                .andExpect(jsonPath("$[1].menu.presentationOrder").value(21))
                .andExpect(jsonPath("$[1].menu.isActive").value(false));
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

        super.mockMvc.perform(post("/rest/menu/elementTest20")
                .content(requestBody)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menu.elementId").value("elementTest20"))
                .andExpect(jsonPath("$.menu.presentationOrder").value(20))
                .andExpect(jsonPath("$.menu.isActive").value(true));
    }
}