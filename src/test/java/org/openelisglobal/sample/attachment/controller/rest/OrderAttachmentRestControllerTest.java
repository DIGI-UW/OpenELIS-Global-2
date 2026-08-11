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

    /**
     * Keep in sync with liquibase
     * 3.5.x.x/072-migrate-result-files-to-order-attachments.xml — the same
     * statement, exercised against seeded data.
     */
    private static final String MIGRATION_SQL = "INSERT INTO clinlims.order_attachment (id, sample_id, analysis_id,"
            + " test_result_component_id, original_file_name, file_type, file_size_bytes, file_content, uploaded_by,"
            + " uploaded_at, is_deleted) SELECT nextval('clinlims.order_attachment_seq'), si.samp_id, a.id, (SELECT"
            + " trc.id FROM clinlims.test_result_component trc WHERE trc.test_id = a.test_id AND trc.is_active = 'Y'"
            + " ORDER BY trc.is_primary DESC, (trc.code = 'PRIMARY') DESC, COALESCE(trc.display_order, 2147483647)"
            + " ASC, trc.id ASC LIMIT 1), rf.file_name, CASE WHEN rf.file_type LIKE 'data:%' THEN"
            + " substring(rf.file_type from 6) ELSE rf.file_type END, octet_length(rf.file_content), rf.file_content,"
            + " NULL, rf.uploaded_at, false FROM clinlims.result_file rf JOIN clinlims.analysis a ON"
            + " a.result_file_id = rf.id JOIN clinlims.sample_item si ON si.id = a.sampitem_id WHERE NOT EXISTS"
            + " (SELECT 1 FROM clinlims.order_attachment oa WHERE oa.analysis_id = a.id AND oa.original_file_name ="
            + " rf.file_name AND oa.file_size_bytes = octet_length(rf.file_content))";

    /**
     * OGC-811 backward compatibility — legacy result_file records (1:1 with the
     * analysis) are migrated to component-level attachments on the analysis's
     * PRIMARY component per the domain rule (is_primary flag over display order),
     * with metadata preserved and the legacy "data:" file-type prefix stripped.
     * Orphaned result_file rows (no analysis reference) are not migrated, the
     * result_file table itself is untouched, and the migration is idempotent.
     */
    @Test
    public void legacyResultFiles_migrateToPrimaryComponentAttachments() throws Exception {
        // a decoy non-primary component with the LOWEST display order — the
        // is_primary flag must win over ordering (pickPrimary rule)
        jdbc.update("INSERT INTO clinlims.test_result_component (id, test_id, code, label, display_order, is_primary,"
                + " is_active, lastupdated) VALUES ('c-att-0', 1, 'DECOY', 'Decoy', 0, false, 'Y', NOW())");
        jdbc.update("UPDATE clinlims.test_result_component SET display_order = 5 WHERE id = 'c-att-1'");
        jdbc.update("INSERT INTO clinlims.result_file (id, file_name, file_type, file_content, uploaded_at,"
                + " last_updated) VALUES (9601, 'legacy.jpg', 'data:image/jpeg', '\\x01020304'::bytea, NOW(), NOW())");
        jdbc.update("INSERT INTO clinlims.result_file (id, file_name, file_type, file_content, uploaded_at,"
                + " last_updated) VALUES (9602, 'orphan.jpg', 'image/jpeg', '\\x05'::bytea, NOW(), NOW())");
        jdbc.update("UPDATE clinlims.analysis SET result_file_id = 9601 WHERE id = 1");

        jdbc.execute(MIGRATION_SQL);

        mockMvc.perform(get("/rest/order/12345/attachments").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].fileName").value("legacy.jpg"))
                .andExpect(jsonPath("$[0].analysisId").value("1"))
                .andExpect(jsonPath("$[0].testResultComponentId").value("c-att-1"))
                .andExpect(jsonPath("$[0].fileType").value("image/jpeg"))
                .andExpect(jsonPath("$[0].fileSizeBytes").value(4));

        // idempotent — re-running migrates nothing new; orphan excluded;
        // result_file untouched
        jdbc.execute(MIGRATION_SQL);
        Integer migrated = jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.order_attachment WHERE is_deleted = false", Integer.class);
        org.junit.Assert.assertEquals(Integer.valueOf(1), migrated);
        Integer legacyRows = jdbc.queryForObject("SELECT count(*) FROM clinlims.result_file", Integer.class);
        org.junit.Assert.assertEquals(Integer.valueOf(2), legacyRows);
    }
}
