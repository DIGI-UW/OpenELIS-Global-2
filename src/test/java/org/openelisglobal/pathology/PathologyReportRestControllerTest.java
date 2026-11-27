package org.openelisglobal.pathology;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Base64;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.config.ControllerSetup;
import org.openelisglobal.program.controller.pathology.PathologyReportRestController;
import org.openelisglobal.program.service.PathologyReportService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * The report endpoints answer the shapes the screen reads, serve a stored file
 * inline at its own address, and find nothing on a case outside the user's lab
 * units (design table R).
 */
public class PathologyReportRestControllerTest extends PathologyControllerTestSupport {

    private static final String REPORTS = "/rest/pathology/caseView/" + CASE_ID + "/reports";

    /** The fixture's report row: on file from before versions, no file. */
    private static final int LEGACY_ROW_ID = 601;

    private StubReportRenderer renderer;

    @Configuration
    static class ReportConfig {

        /** Built over the parent's own services, as the support builds the others. */
        @Bean
        PathologyReportRestController pathologyReportRestController(ApplicationContext context) {
            return context.getParent().getAutowireCapableBeanFactory().createBean(PathologyReportRestController.class);
        }

        /**
         * The application's controller advice, which answers a refused attachment; the
         * test application does not scan its package.
         */
        @Bean
        ControllerSetup controllerSetup(ApplicationContext context) {
            return context.getParent().getAutowireCapableBeanFactory().createBean(ControllerSetup.class);
        }
    }

    @Before
    public void addTheReportEndpoints() {
        securityContext.close();
        securityContext = new AnnotationConfigWebApplicationContext();
        securityContext.setParent(webApplicationContext);
        securityContext.setServletContext(webApplicationContext.getServletContext());
        securityContext.register(TestConfig.class, ReportConfig.class);
        securityContext.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(securityContext).apply(springSecurity()).build();
        clearReportRows();
        renderer = StubReportRenderer.installIn(webApplicationContext.getBean(PathologyReportService.class));
    }

    @After
    public void restoreTheTemplate() {
        renderer.restore();
        clearReportRows();
    }

    @Test
    public void eachEndpoint_answersItsShape() throws Exception {
        Map<String, Object> generated = bodyOf(postAsTechnician(REPORTS + "/generate", null));
        assertEquals("a generate answers the stored version", 1, generated.get("version"));
        assertEquals("DRAFT", generated.get("type"));
        assertEquals("CURRENT", generated.get("state"));
        assertTrue("and never the file", !generated.containsKey("image") && !generated.containsKey("bytes"));

        MockHttpServletResponse uploaded = postAsTechnician(REPORTS, pngBody());
        assertEquals(HttpStatus.OK.value(), uploaded.getStatus());
        Map<String, Object> upload = bodyOf(uploaded);
        assertEquals("an upload answers its version", 2, upload.get("version"));
        assertEquals("EXTERNAL", upload.get("type"));

        MockHttpServletResponse stalePage = postAsTechnician(REPORTS,
                pngBody().replace("{", "{\"reportType\":\"PATHOLOGY\",\"versionNumber\":42,\"voided\":true,"));
        assertEquals("a body naming the old type and server-set fields is taken without them", HttpStatus.OK.value(),
                stalePage.getStatus());
        Map<String, Object> stale = bodyOf(stalePage);
        assertEquals("as an upload", "EXTERNAL", stale.get("type"));
        assertEquals("at the next version, not the one it names", 3, stale.get("version"));
        assertEquals("and in force", "CURRENT", stale.get("state"));

        int uploadId = (Integer) upload.get("id");
        Map<String, Object> withdrawn = bodyOf(postAsTechnician(REPORTS + "/" + uploadId + "/withdraw", null));
        assertEquals("a withdrawal answers the row, voided", "VOIDED", withdrawn.get("state"));

        MockHttpServletResponse again = postAsTechnician(REPORTS + "/" + uploadId + "/withdraw", null);
        assertEquals("a second withdrawal is a conflict", HttpStatus.CONFLICT.value(), again.getStatus());
        assertEquals("named as the report's", "report", bodyOf(again).get("conflict"));

        MockHttpServletResponse notWithdrawable = postAsTechnician(REPORTS + "/" + generated.get("id") + "/withdraw",
                null);
        assertEquals("a refusal is a 400", HttpStatus.BAD_REQUEST.value(), notWithdrawable.getStatus());
        assertEquals("with the message the screen translates", "pathology.error.reportNotWithdrawable",
                bodyOf(notWithdrawable).get("messageKey"));

        MockHttpServletResponse refusedFile = postAsTechnician(REPORTS,
                "{\"base64Image\":\"data:text/html;base64,PHNjcmlwdD4=\"}");
        assertEquals("a refused file is a 400", HttpStatus.BAD_REQUEST.value(), refusedFile.getStatus());
        assertEquals("with the attachment hint", "clinicalAttachment.uploadHint",
                bodyOf(refusedFile).get("messageKey"));

        assertEquals("a row id that is a word is a malformed address", HttpStatus.BAD_REQUEST.value(),
                getAsTechnician(REPORTS + "/abc/file").getStatus());
        assertEquals("a row that holds no file is not found", HttpStatus.NOT_FOUND.value(),
                getAsTechnician(REPORTS + "/" + LEGACY_ROW_ID + "/file").getStatus());
    }

    @Test
    public void theFile_isInline_withNosniff_andNoStore() throws Exception {
        int reportId = (Integer) bodyOf(postAsTechnician(REPORTS + "/generate", null)).get("id");

        MockHttpServletResponse file = getAsTechnician(REPORTS + "/" + reportId + "/file");

        assertEquals(HttpStatus.OK.value(), file.getStatus());
        assertArrayEquals("the stored bytes", StubReportRenderer.BODY, file.getContentAsByteArray());
        assertEquals("served as its stored type", "application/pdf", file.getContentType());
        assertEquals("inline, named by lab number and version", "inline; filename=\"" + accessionNumber() + "-v1.pdf\"",
                file.getHeader("Content-Disposition"));
        assertEquals("never sniffed", "nosniff", file.getHeader("X-Content-Type-Options"));
        String cacheControl = file.getHeader("Cache-Control");
        assertTrue("never stored by a shared cache: " + cacheControl,
                cacheControl.contains("no-store") && cacheControl.contains("private"));
    }

    // R13
    @Test
    public void aUserOutsideTheUnit_gets404_onEachEndpoint() throws Throwable {
        int reportId = (Integer) bodyOf(postAsTechnician(REPORTS + "/generate", null)).get("id");
        int rendersBefore = renderer.amendedSeen.size();

        asAUserOutsideTheCaseUnits(() -> {
            assertEquals("generate", HttpStatus.NOT_FOUND.value(),
                    postAsTechnician(REPORTS + "/generate", null).getStatus());
            assertEquals("upload", HttpStatus.NOT_FOUND.value(), postAsTechnician(REPORTS, pngBody()).getStatus());
            assertEquals("the file", HttpStatus.NOT_FOUND.value(),
                    getAsTechnician(REPORTS + "/" + reportId + "/file").getStatus());
            assertEquals("withdraw", HttpStatus.NOT_FOUND.value(),
                    postAsTechnician(REPORTS + "/" + reportId + "/withdraw", null).getStatus());
        });

        assertEquals("nothing reached the template", rendersBefore, renderer.amendedSeen.size());
        assertEquals("and no row was written", Integer.valueOf(1),
                jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.pathology_report WHERE"
                        + " pathology_sample_id = ? AND image IS NOT NULL", Integer.class, CASE_ID));
    }

    // helpers

    private MockHttpServletResponse getAsTechnician(String endpoint) throws Exception {
        return mockMvc.perform(get(endpoint).with(user("technician1"))).andReturn().getResponse();
    }

    private static String pngBody() {
        byte[] png = { (byte) 137, 80, 78, 71, 13, 10, 26, 10, 1, 2, 3 };
        return "{\"base64Image\":\"data:image/png;base64," + Base64.getEncoder().encodeToString(png) + "\"}";
    }

    private String accessionNumber() {
        return jdbcTemplate.queryForObject(
                "SELECT s.accession_number FROM clinlims.sample s"
                        + " JOIN clinlims.pathology_sample ps ON ps.sample_id = s.id WHERE ps.id = ?",
                String.class, CASE_ID);
    }

    private void clearReportRows() {
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_table ="
                + " (SELECT id FROM clinlims.reference_tables WHERE name = 'PATHOLOGY_REPORT')");
        jdbcTemplate.update("DELETE FROM clinlims.pathology_report WHERE pathology_sample_id = ? AND id <> ?", CASE_ID,
                LEGACY_ROW_ID);
    }
}
