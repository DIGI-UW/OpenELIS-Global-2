package org.openelisglobal.qaevent.qiconfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qaevent.qiconfig.dto.QiConfigView;
import org.openelisglobal.qaevent.qiconfig.dto.ResolvedConfig;
import org.openelisglobal.qaevent.qiconfig.service.QiConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-709 — QI Configuration behaviour against a real DB (no mocks). Covers the
 * resolve contract consumers key off (default / override-wins / disabled
 * short-circuit / NCE nulls / unknown), the write-side validation guards, the
 * delete-by-omission of overrides, and the partial-unique-default constraint
 * the API cannot reach.
 *
 * <p>
 * Two things are deliberately NOT asserted here and are covered by UAT instead:
 * (1) the 403 permission path — bypassed under direct controller invocation
 * (Spring Security proxy), matching the sibling editor ITs; (2) audit-trail
 * history writes — the generic {@code clinlims.history} audit is not exercised
 * by this test harness (no repo test verifies it), and the wiring here is
 * identical to the shipped {@code NceActionLogServiceImpl}. See UAT step 21.
 *
 * <p>
 * The shipped Liquibase seed (qi-config-003) is truncated by the base test
 * harness, so each test seeds the config it needs rather than depending on it.
 */
public class QiConfigServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String SECTION_A = "302"; // Hematology (from fixture)
    private static final String SECTION_B = "300"; // Microbiology
    private static final String USER = "1";

    @Autowired
    private QiConfigService qiConfigService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/user-test-section.xml");
        jdbc.update("DELETE FROM clinlims.qi_config"); // clean slate; tests self-seed
    }

    @After
    public void tearDown() {
        jdbc.update("DELETE FROM clinlims.qi_config");
    }

    // ---- resolve contract (the read contract 710/711/712 consume) ----

    @Test
    public void resolve_noOverride_returnsDefault() {
        qiConfigService.saveIndicator("REJECTION", view(true, bd("2"), bd("5")), USER);
        ResolvedConfig bySection = qiConfigService.resolve("REJECTION", SECTION_A);
        ResolvedConfig noSection = qiConfigService.resolve("REJECTION", null);
        assertEquals(0, bySection.getTarget().compareTo(bd("2")));
        assertEquals(0, noSection.getTarget().compareTo(bd("2")));
        assertTrue(bySection.isEnabled());
        assertEquals("LOWER_BETTER", bySection.getDirection());
    }

    @Test
    public void resolve_overrideWinsForItsSectionOnly() {
        qiConfigService.saveIndicator("REJECTION", view(true, bd("2"), bd("5"), ov(SECTION_A, bd("1"), bd("3"))), USER);
        assertEquals(0, qiConfigService.resolve("REJECTION", SECTION_A).getTarget().compareTo(bd("1")));
        assertEquals(0, qiConfigService.resolve("REJECTION", SECTION_B).getTarget().compareTo(bd("2")));
    }

    @Test
    public void resolve_disabledDefault_shortCircuitsAndIgnoresOverride() {
        // Even with an override present, a disabled default returns the DEFAULT's
        // values and enabled=false (711 treats it as not-tracked).
        qiConfigService.saveIndicator("REJECTION", view(false, bd("2"), bd("5"), ov(SECTION_A, bd("1"), bd("3"))),
                USER);
        ResolvedConfig r = qiConfigService.resolve("REJECTION", SECTION_A);
        assertFalse(r.isEnabled());
        assertEquals(0, r.getTarget().compareTo(bd("2")));
    }

    @Test
    public void resolve_nce_nullThresholds_noNpe() {
        qiConfigService.saveIndicator("NCE", view(true, null, null), USER);
        ResolvedConfig r = qiConfigService.resolve("NCE", null);
        assertTrue(r.isEnabled());
        assertNull(r.getTarget());
        assertNull(r.getAction());
        assertEquals("LOWER_BETTER", r.getDirection());
    }

    @Test
    public void resolve_unknownIndicator_throws() {
        assertThrows(IllegalArgumentException.class, () -> qiConfigService.resolve("BOGUS", null));
    }

    // ---- getAllConfigs shape ----

    @Test
    public void getAllConfigs_returnsAllFour_withOverrideNamePopulated() {
        qiConfigService.saveIndicator("AMENDMENT", view(true, bd("0.5"), bd("2"), ov(SECTION_A, bd("0.2"), bd("1"))),
                USER);
        List<QiConfigView> all = qiConfigService.getAllConfigs();
        assertEquals(4, all.size());
        QiConfigView amendment = all.stream().filter(v -> v.getIndicatorKey().equals("AMENDMENT")).findFirst()
                .orElseThrow();
        assertEquals(1, amendment.getOverrides().size());
        assertEquals(SECTION_A, amendment.getOverrides().get(0).getTestCategoryId());
        assertNotNull(amendment.getOverrides().get(0).getTestSectionName());
    }

    @Test
    public void saveIndicator_omittingOverride_deletesIt() {
        qiConfigService.saveIndicator("AMENDMENT", view(true, bd("0.5"), bd("2"), ov(SECTION_A, bd("0.2"), bd("1"))),
                USER);
        qiConfigService.saveIndicator("AMENDMENT", view(true, bd("0.5"), bd("2")), USER); // no overrides
        QiConfigView amendment = qiConfigService.getAllConfigs().stream()
                .filter(v -> v.getIndicatorKey().equals("AMENDMENT")).findFirst().orElseThrow();
        assertTrue(amendment.getOverrides().isEmpty());
    }

    // ---- validation guards (all → IllegalArgumentException → HTTP 400) ----

    @Test
    public void save_unknownIndicator_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> qiConfigService.saveIndicator("BOGUS", view(true, bd("2"), bd("5")), USER));
    }

    @Test
    public void save_outOfRange_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> qiConfigService.saveIndicator("REJECTION", view(true, bd("2"), bd("150")), USER));
    }

    @Test
    public void save_wrongDirection_throws() {
        // REJECTION is lower-better: target must be < action.
        assertThrows(IllegalArgumentException.class,
                () -> qiConfigService.saveIndicator("REJECTION", view(true, bd("8"), bd("5")), USER));
        // TAT is higher-better: target must be > action.
        assertThrows(IllegalArgumentException.class,
                () -> qiConfigService.saveIndicator("TAT", view(true, bd("70"), bd("85")), USER));
    }

    @Test
    public void save_duplicateOverrideSection_throws() {
        assertThrows(IllegalArgumentException.class, () -> qiConfigService.saveIndicator("REJECTION",
                view(true, bd("2"), bd("5"), ov(SECTION_A, bd("1"), bd("3")), ov(SECTION_A, bd("1"), bd("4"))), USER));
    }

    @Test
    public void save_overrideMissingSection_throws() {
        assertThrows(IllegalArgumentException.class, () -> qiConfigService.saveIndicator("REJECTION",
                view(true, bd("2"), bd("5"), ov(null, bd("1"), bd("3"))), USER));
    }

    // ---- the constraint the bundle PUT cannot reach: a second indicator default
    // ----

    @Test
    public void secondDefaultRow_violatesPartialUniqueIndex() {
        // One default exists...
        jdbc.update("INSERT INTO clinlims.qi_config (id, indicator_key, is_enabled, target_threshold, action_threshold)"
                + " VALUES (nextval('clinlims.qi_config_id_seq'), 'REJECTION', true, 2, 5)");
        // ...a second default for the same indicator must fail on uq_qi_config_default.
        assertThrows(Exception.class, () -> jdbc.update("INSERT INTO clinlims.qi_config (id, indicator_key, is_enabled)"
                + " VALUES (nextval('clinlims.qi_config_id_seq'), 'REJECTION', true)"));
    }

    // ---- helpers ----

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    private static QiConfigView view(boolean enabled, BigDecimal target, BigDecimal action,
            QiConfigView.Override... overrides) {
        QiConfigView v = new QiConfigView();
        v.setEnabled(enabled);
        v.setTarget(target);
        v.setAction(action);
        for (QiConfigView.Override o : overrides) {
            v.getOverrides().add(o);
        }
        return v;
    }

    private static QiConfigView.Override ov(String sectionId, BigDecimal target, BigDecimal action) {
        QiConfigView.Override o = new QiConfigView.Override();
        o.setTestCategoryId(sectionId);
        o.setTarget(target);
        o.setAction(action);
        return o;
    }
}
