package org.openelisglobal.sample.attachment.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * OGC-811 — order attachments are the single attachments API for order entry
 * AND both Results pages. Uploads from a Results page carry the analysis (and,
 * for multi-component tests, the result component) they document; order-entry
 * uploads carry neither and stay order-level. The scope is persisted and
 * returned by the API — component isolation is a backend fact, not a frontend
 * filter.
 */
public class OrderAttachmentRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private MockHttpSession session;

    private static final MockMultipartFile PDF = new MockMultipartFile("files", "report.pdf", "application/pdf",
            "%PDF-1.4 test".getBytes());

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/result.xml");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO clinlims.test_result_component (id, test_id, code, label, is_primary, is_active,"
                + " lastupdated) VALUES ('c-att-1', 1, 'HGB', 'Hemoglobin', true, 'Y', NOW())");
        session = buildAuthenticatedSession();
    }

    private MockHttpSession buildAuthenticatedSession() {
        UserDetails userDetails = User.withUsername("admin").password("N/A").authorities("ROLE_ADMIN", "ROLE_RESULTS")
                .build();
        SecurityContext sc = new SecurityContextImpl();
        sc.setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        return httpSession;
    }

    @Test
    public void upload_scopedToAnalysisAndComponent_persistsAndReturnsScope() throws Exception {
        mockMvc.perform(multipart("/rest/order/12345/attachments").file(PDF).param("analysisId", "1")
                .param("testResultComponentId", "c-att-1").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].fileName").value("report.pdf"))
                .andExpect(jsonPath("$[0].analysisId").value("1"))
                .andExpect(jsonPath("$[0].testResultComponentId").value("c-att-1"));

        mockMvc.perform(get("/rest/order/12345/attachments").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].analysisId").value("1"))
                .andExpect(jsonPath("$[0].testResultComponentId").value("c-att-1"));
    }

    @Test
    public void upload_withoutScope_staysOrderLevel() throws Exception {
        mockMvc.perform(multipart("/rest/order/12345/attachments").file(PDF).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].analysisId").value(""))
                .andExpect(jsonPath("$[0].testResultComponentId").value(""));
    }

    @Test
    public void componentScopes_areIndependent_perAttachment() throws Exception {
        jdbc.update("INSERT INTO clinlims.test_result_component (id, test_id, code, label, is_primary, is_active,"
                + " lastupdated) VALUES ('c-att-2', 1, 'WBC', 'WBC', false, 'Y', NOW())");
        MockMultipartFile second = new MockMultipartFile("files", "wbc.png", "image/png", new byte[] { 1, 2, 3 });

        mockMvc.perform(multipart("/rest/order/12345/attachments").file(PDF).param("analysisId", "1")
                .param("testResultComponentId", "c-att-1").session(session)).andExpect(status().isOk());
        mockMvc.perform(multipart("/rest/order/12345/attachments").file(second).param("analysisId", "1")
                .param("testResultComponentId", "c-att-2").session(session)).andExpect(status().isOk());

        mockMvc.perform(get("/rest/order/12345/attachments").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.fileName == 'report.pdf')].testResultComponentId").value("c-att-1"))
                .andExpect(jsonPath("$[?(@.fileName == 'wbc.png')].testResultComponentId").value("c-att-2"));
    }

    @Test
    public void upload_nonNumericAnalysisId_isRejected400() throws Exception {
        mockMvc.perform(
                multipart("/rest/order/12345/attachments").file(PDF).param("analysisId", "abc").session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void upload_disallowedType_isRejected400() throws Exception {
        MockMultipartFile exe = new MockMultipartFile("files", "evil.exe", "application/octet-stream",
                new byte[] { 1 });
        mockMvc.perform(multipart("/rest/order/12345/attachments").file(exe).session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void upload_unknownAccession_is404() throws Exception {
        mockMvc.perform(multipart("/rest/order/NOPE/attachments").file(PDF).session(session))
                .andExpect(status().isNotFound());
    }
}
