package org.openelisglobal.textmacro.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.textmacro.controller.rest.TextMacroAdminRestController;
import org.openelisglobal.textmacro.controller.rest.TextMacroRestController;
import org.openelisglobal.textmacro.form.TextMacroAdminForm;
import org.openelisglobal.textmacro.form.TextMacroBulkRequestForm;
import org.openelisglobal.textmacro.form.TextMacroListForm;
import org.openelisglobal.textmacro.form.TextMacroSummaryForm;
import org.openelisglobal.textmacro.service.TextMacroRequestException;
import org.openelisglobal.textmacro.service.TextMacroService;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

public class TextMacroRestControllerTest {

    @Test
    public void adminWriteUsesAuthenticatedActor() {
        TextMacroService service = mock(TextMacroService.class);
        TextMacroAdminRestController controller = new TextMacroAdminRestController(service);
        TextMacroAdminForm request = new TextMacroAdminForm();
        request.code = ".gpc";
        request.expansionText = "Gram-positive cocci";

        controller.create(requestFor("42"), request);

        verify(service).save(null, request, "42");
    }

    @Test
    public void adminBulkWriteUsesAuthenticatedActor() {
        TextMacroService service = mock(TextMacroService.class);
        TextMacroAdminRestController controller = new TextMacroAdminRestController(service);
        TextMacroBulkRequestForm request = new TextMacroBulkRequestForm();
        request.action = "DEACTIVATE";
        request.ids = List.of("macro-1");

        controller.bulk(requestFor("42"), request);

        verify(service).bulk(request, "42");
    }

    @Test
    public void exportUsesStableCsvAttachmentContract() {
        TextMacroService service = mock(TextMacroService.class);
        when(service.exportCsv()).thenReturn("code,expansion_text\r\n.gpc,Text\r\n");

        org.springframework.http.ResponseEntity<String> response = new TextMacroAdminRestController(service).export();

        assertEquals("text/csv;charset=UTF-8", response.getHeaders().getContentType().toString());
        assertEquals("attachment; filename=\"openelis-text-macros.csv\"",
                response.getHeaders().getFirst(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION));
        assertEquals("code,expansion_text\r\n.gpc,Text\r\n", response.getBody());
    }

    @Test
    public void requestContractHasNoClientControlledActor() {
        for (Field field : TextMacroAdminForm.class.getDeclaredFields()) {
            assertFalse("actor must not be client controlled",
                    field.getName().equalsIgnoreCase("actor") || field.getName().equalsIgnoreCase("performedBy")
                            || field.getName().equalsIgnoreCase("lastUpdatedBy"));
        }
    }

    @Test
    public void runtimeReadAndAdministrationUseSeparateAuthorization() {
        assertEquals("isAuthenticated()", TextMacroRestController.class.getAnnotation(PreAuthorize.class).value());
        assertEquals("hasRole('ADMIN')", TextMacroAdminRestController.class.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    public void runtimeLookupUsesStableItemsEnvelope() {
        TextMacroService service = mock(TextMacroService.class);
        TextMacroSummaryForm macro = new TextMacroSummaryForm();
        macro.code = ".gpc";
        when(service.findActive("MICROBIOLOGY_CULTURE_ACTIVITY", "", 20)).thenReturn(List.of(macro));

        TextMacroListForm response = new TextMacroRestController(service)
                .findActive("MICROBIOLOGY_CULTURE_ACTIVITY", "", 20).getBody();

        assertEquals(1, response.items.size());
        assertEquals(".gpc", response.items.get(0).code);
    }

    @Test
    public void invalidRequestsExposeStableErrorCode() throws Exception {
        TextMacroService service = mock(TextMacroService.class);
        when(service.save(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("42")))
                .thenThrow(new TextMacroRequestException("INVALID_MACRO_CODE", "Invalid shortcut code"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new TextMacroAdminRestController(service)).build();

        mvc.perform(post("/rest/text-macros/admin")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionDataFor("42"))
                .contentType(MediaType.APPLICATION_JSON).content(
                        "{\"code\":\"invalid!\",\"expansionText\":\"Text\",\"contexts\":[\"MICROBIOLOGY_CULTURE_ACTIVITY\"]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("INVALID_MACRO_CODE"));
    }

    @Test
    public void controllersKeepTransactionsInServiceLayer() {
        assertNull(TextMacroRestController.class.getAnnotation(Transactional.class));
        assertNull(TextMacroAdminRestController.class.getAnnotation(Transactional.class));
    }

    private MockHttpServletRequest requestFor(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionDataFor(userId));
        return request;
    }

    private UserSessionData sessionDataFor(String userId) {
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        return sessionData;
    }
}
