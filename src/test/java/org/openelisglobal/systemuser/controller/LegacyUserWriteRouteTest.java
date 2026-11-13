package org.openelisglobal.systemuser.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class LegacyUserWriteRouteTest {
    @Test
    public void legacyFormCannotWriteAccountsOrRoles() throws Exception {
        MockMvcBuilders.standaloneSetup(new UnifiedSystemUserController()).build()
                .perform(post("/UnifiedSystemUser").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("systemUserId", "1").param("selectedRoles", "1"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void legacyFormCannotDeleteAccounts() throws Exception {
        MockMvcBuilders
                .standaloneSetup(new UnifiedSystemUserMenuController()).build().perform(post("/DeleteUnifiedSystemUser")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("selectedIDs", "4-7"))
                .andExpect(status().isNotFound());
    }
}
