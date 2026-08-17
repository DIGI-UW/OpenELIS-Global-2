package org.openelisglobal.accreditation;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.accreditation.dto.AccreditationReportData;
import org.openelisglobal.accreditation.service.AccreditationReportService;
import org.openelisglobal.accreditation.service.AccreditingBodyService;
import org.openelisglobal.accreditation.service.TestAccreditationService;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.accreditation.valueholder.LogoVisibilityMode;
import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.image.valueholder.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-686 — the patient-report accreditation gate, against a real DB (no
 * mocks).
 *
 * <p>
 * These are the report-preview scenarios, run against the real resolver: which
 * bodies get a logo, which only get named in the notes line, and which are
 * excluded outright. The two that matter most for a compliance artifact are the
 * reprint case (a body that has expired since the report was released must
 * still print exactly as it did) and the distinct-test case (a test repeated
 * across samples must not inflate a percentage gate).
 *
 * <p>
 * Jasper is not involved: the resolver returns bytes and a string, and the
 * report layer does nothing with them but put them in the parameter map.
 */
public class AccreditationReportServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String TEST_GLUCOSE = "9101";
    private static final String TEST_SODIUM = "9102";
    private static final String USER = "1";
    private static final LocalDate TODAY = LocalDate.now();

    @Autowired
    private AccreditationReportService accreditationReportService;

    @Autowired
    private AccreditingBodyService accreditingBodyService;

    @Autowired
    private TestAccreditationService testAccreditationService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/accreditation.xml");
        clean();
    }

    @After
    public void tearDown() {
        clean();
    }

    private void clean() {
        jdbc.update("DELETE FROM clinlims.test_accreditation");
        jdbc.update("DELETE FROM clinlims.accrediting_body");
    }

    @Test
    public void noTestsOnReport_resolvesToNothing() {
        Long body = createBody("ISO15189", "ISO 15189", TODAY.plusYears(1), LogoVisibilityMode.ANY_ACCREDITED_TEST,
                (short) 80, (short) 0);
        enrollWithLogo(body, TEST_GLUCOSE);

        AccreditationReportData resolved = accreditationReportService.resolve(Collections.emptyList(), TODAY);

        assertTrue(resolved.getLogos().isEmpty());
        assertNull(resolved.getNotesLine());
    }

    @Test
    public void noAccreditedTestOnReport_resolvesToNothing() {
        Long body = createBody("ISO15189", "ISO 15189", TODAY.plusYears(1), LogoVisibilityMode.ANY_ACCREDITED_TEST,
                (short) 80, (short) 0);
        enrollWithLogo(body, TEST_GLUCOSE);

        AccreditationReportData resolved = accreditationReportService.resolve(List.of(TEST_SODIUM), TODAY);

        assertTrue(resolved.getLogos().isEmpty());
        assertNull(resolved.getNotesLine());
    }

    @Test
    public void anyMode_oneAccreditedTestOfTwo_printsLogoAndNamesBody() {
        Long body = createBody("ISO15189", "ISO 15189", TODAY.plusYears(1), LogoVisibilityMode.ANY_ACCREDITED_TEST,
                (short) 80, (short) 0);
        byte[] logo = enrollWithLogo(body, TEST_GLUCOSE);

        AccreditationReportData resolved = accreditationReportService.resolve(List.of(TEST_GLUCOSE, TEST_SODIUM),
                TODAY);

        assertEquals(1, resolved.getLogos().size());
        assertArrayEquals(logo, resolved.getLogos().get(0));
        assertTrue(resolved.getNotesLine(), resolved.getNotesLine().contains("ISO 15189"));
    }

    @Test
    public void percentageMode_belowThreshold_namesBodyButSuppressesLogo() {
        Long body = createBody("SANAS", "SANAS General", TODAY.plusYears(1), LogoVisibilityMode.PERCENTAGE, (short) 80,
                (short) 0);
        enrollWithLogo(body, TEST_GLUCOSE);

        // 1 of 2 tests accredited = 50%, under the body's 80% rule.
        AccreditationReportData resolved = accreditationReportService.resolve(List.of(TEST_GLUCOSE, TEST_SODIUM),
                TODAY);

        assertTrue("logo is a claim about this report; 50% does not earn it", resolved.getLogos().isEmpty());
        assertTrue("the lab still holds the accreditation, so the line names it",
                resolved.getNotesLine().contains("SANAS General"));
    }

    @Test
    public void percentageMode_exactlyAtThreshold_printsLogo() {
        Long body = createBody("SANAS", "SANAS General", TODAY.plusYears(1), LogoVisibilityMode.PERCENTAGE, (short) 50,
                (short) 0);
        byte[] logo = enrollWithLogo(body, TEST_GLUCOSE);

        AccreditationReportData resolved = accreditationReportService.resolve(List.of(TEST_GLUCOSE, TEST_SODIUM),
                TODAY);

        assertEquals(1, resolved.getLogos().size());
        assertArrayEquals(logo, resolved.getLogos().get(0));
    }

    @Test
    public void percentageMode_repeatedTestCountsOnce() {
        Long body = createBody("SANAS", "SANAS General", TODAY.plusYears(1), LogoVisibilityMode.PERCENTAGE, (short) 40,
                (short) 0);
        enrollWithLogo(body, TEST_GLUCOSE);

        // The same test on two samples of one report. Deduped: 1 of 2 = 50% >= 40, so
        // the logo prints. Counting the repeat as a third test would give 33% and
        // silently drop it.
        AccreditationReportData resolved = accreditationReportService
                .resolve(Arrays.asList(TEST_GLUCOSE, TEST_GLUCOSE, TEST_SODIUM), TODAY);

        assertEquals(1, resolved.getLogos().size());
    }

    @Test
    public void expiredBody_isExcludedEntirely() {
        Long body = createBody("OLD", "Lapsed Body", TODAY.minusDays(1), LogoVisibilityMode.ANY_ACCREDITED_TEST,
                (short) 80, (short) 0);
        enrollWithLogo(body, TEST_GLUCOSE);

        AccreditationReportData resolved = accreditationReportService.resolve(List.of(TEST_GLUCOSE), TODAY);

        assertTrue(resolved.getLogos().isEmpty());
        assertNull("an expired accreditation may not be claimed at all", resolved.getNotesLine());
    }

    @Test
    public void expiringBody_stillPrints() {
        Long body = createBody("SOON", "Renewing Body", TODAY.plusDays(30), LogoVisibilityMode.ANY_ACCREDITED_TEST,
                (short) 80, (short) 0);
        enrollWithLogo(body, TEST_GLUCOSE);

        AccreditationReportData resolved = accreditationReportService.resolve(List.of(TEST_GLUCOSE), TODAY);

        assertEquals("EXPIRING warns the lab, not the report reader", 1, resolved.getLogos().size());
    }

    @Test
    public void reprintAfterExpiry_reproducesTheOriginalPdf() {
        Long body = createBody("ISO15189", "ISO 15189", TODAY.minusDays(10), LogoVisibilityMode.ANY_ACCREDITED_TEST,
                (short) 80, (short) 0);
        enrollWithLogo(body, TEST_GLUCOSE);

        // Released while the accreditation was in force; reprinted today, after it
        // lapsed. The gate keys off the release date, so the PDF is unchanged.
        AccreditationReportData resolved = accreditationReportService.resolve(List.of(TEST_GLUCOSE),
                TODAY.minusDays(40));

        assertEquals(1, resolved.getLogos().size());
        assertTrue(resolved.getNotesLine().contains("ISO 15189"));
    }

    @Test
    public void inactiveBody_isExcludedEntirely() {
        AccreditingBody input = body("ISO15189", "ISO 15189", TODAY.plusYears(1));
        input.setActive(Boolean.FALSE);
        Long id = accreditingBodyService.createBody(input, USER).getId();
        enrollWithLogo(id, TEST_GLUCOSE);

        AccreditationReportData resolved = accreditationReportService.resolve(List.of(TEST_GLUCOSE), TODAY);

        assertTrue(resolved.getLogos().isEmpty());
        assertNull(resolved.getNotesLine());
    }

    @Test
    public void bodyWithoutLogo_contributesNotesLineOnly() {
        Long body = createBody("ISO15189", "ISO 15189", TODAY.plusYears(1), LogoVisibilityMode.ANY_ACCREDITED_TEST,
                (short) 80, (short) 0);
        testAccreditationService.enroll(TEST_GLUCOSE, body, null, USER);

        AccreditationReportData resolved = accreditationReportService.resolve(List.of(TEST_GLUCOSE), TODAY);

        assertTrue(resolved.getLogos().isEmpty());
        assertTrue(resolved.getNotesLine().contains("ISO 15189"));
    }

    @Test
    public void moreQualifyingBodiesThanSlots_takesTheFirstThreeInDisplayOrder() {
        byte[] fourth = null;
        for (int order = 1; order <= 4; order++) {
            Long body = createBody("BODY" + order, "Body " + order, TODAY.plusYears(1),
                    LogoVisibilityMode.ANY_ACCREDITED_TEST, (short) 80, (short) order);
            byte[] logo = enrollWithLogo(body, TEST_GLUCOSE);
            if (order == 4) {
                fourth = logo;
            }
        }

        AccreditationReportData resolved = accreditationReportService.resolve(List.of(TEST_GLUCOSE), TODAY);

        assertEquals("only three template slots exist", 3, resolved.getLogos().size());
        for (byte[] printed : resolved.getLogos()) {
            assertTrue("the dropped logo must be the last in display order", !Arrays.equals(fourth, printed));
        }
        // Every qualifying body is still named, slots or not.
        assertTrue(resolved.getNotesLine(), resolved.getNotesLine().contains("Body 4"));
    }

    // ---- helpers ----

    /** Enrolls the test under the body and gives the body a distinct logo. */
    private byte[] enrollWithLogo(Long bodyId, String testId) {
        testAccreditationService.enroll(testId, bodyId, null, USER);
        byte[] bytes = ("logo-for-body-" + bodyId).getBytes();
        Image image = new Image();
        image.setImage(bytes);
        image.setDescription("accreditation-logo-body-" + bodyId);
        accreditingBodyService.setLogo(bodyId, imageService.save(image).getId(), USER);
        return bytes;
    }

    private Long createBody(String code, String name, LocalDate expiresOn, LogoVisibilityMode mode, Short threshold,
            Short displayOrder) {
        AccreditingBody input = body(code, name, expiresOn);
        input.setLogoVisibilityMode(mode);
        input.setThresholdPct(threshold);
        input.setDisplayOrder(displayOrder);
        return accreditingBodyService.createBody(input, USER).getId();
    }

    private AccreditingBody body(String code, String name, LocalDate expiresOn) {
        AccreditingBody b = new AccreditingBody();
        b.setCode(code);
        b.setName(name);
        b.setExpiresOn(expiresOn);
        return b;
    }
}
