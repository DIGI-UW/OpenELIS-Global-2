package org.openelisglobal.labelpreset.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.labelpreset.dao.LabelPresetDAO;
import org.openelisglobal.labelpreset.form.TestLabelConfigForm;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.openelisglobal.labelpreset.valueholder.TestLabelConfig;
import org.openelisglobal.labelpreset.valueholder.TestLabelPresetLink;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Service-layer test for {@link TestLabelConfigServiceImpl} +
 * {@link TestLabelPresetLinkServiceImpl#assertPerSamplePreset} (OGC-285 M4).
 *
 * <p>
 * Uses real DAOs + real DB (BaseWebContextSensitiveTest). No mocks; exercises
 * the validation logic end-to-end per the anti-mocking discipline.
 * </p>
 *
 * <p>
 * Key inversion-test target: if {@code assertPerSamplePreset} is
 * removed/gutted, the order-only-preset validation test below must fail.
 * </p>
 */
public class TestLabelConfigServiceImplTest extends BaseWebContextSensitiveTest {

    private static final String SVC_PRESET_PER_SAMPLE = "T103-SvcPerSample";
    private static final String SVC_PRESET_ORDER_ONLY = "T103-SvcOrderOnly";

    @Autowired
    private TestLabelConfigService testLabelConfigService;

    @Autowired
    private TestLabelPresetLinkService testLabelPresetLinkService;

    @Autowired
    private LabelPresetDAO labelPresetDAO;

    @Autowired
    private TestService testService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private LabelPreset perSamplePreset;
    private LabelPreset orderOnlyPreset;
    private org.openelisglobal.test.valueholder.Test bloodTest;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/test.xml");
        executeDataSetWithStateManagement("testdata/system-user.xml");
        cleanTestData();

        bloodTest = testService.getTestById("1");
        assertNotNull("test.xml must supply Test id=1", bloodTest);

        perSamplePreset = saveLabelPreset(SVC_PRESET_PER_SAMPLE, false, true);
        orderOnlyPreset = saveLabelPreset(SVC_PRESET_ORDER_ONLY, true, false);
    }

    @After
    public void tearDown() {
        cleanTestData();
    }

    private void cleanTestData() {
        try {
            jdbcTemplate.update(
                    "DELETE FROM test_label_preset_link WHERE preset_id IN"
                            + " (SELECT id FROM label_preset WHERE name IN (?, ?))",
                    SVC_PRESET_PER_SAMPLE, SVC_PRESET_ORDER_ONLY);
            jdbcTemplate.update("DELETE FROM test_label_config WHERE test_id = 1");
            jdbcTemplate.update("DELETE FROM label_preset WHERE name IN (?, ?)", SVC_PRESET_PER_SAMPLE,
                    SVC_PRESET_ORDER_ONLY);
        } catch (Exception e) {
            // ignored — next run retries
        }
    }

    private LabelPreset saveLabelPreset(String name, boolean printsPerOrder, boolean printsPerSample) {
        LabelPreset preset = new LabelPreset();
        preset.setName(name);
        preset.setHeightMm(25);
        preset.setWidthMm(50);
        preset.setBarcodeType(BarcodeType.CODE_128);
        preset.setPrintsPerOrder(printsPerOrder);
        preset.setPrintsPerSample(printsPerSample);
        preset.setDefaultPerOrder(0);
        preset.setMaxPerOrder(10);
        preset.setDefaultPerSample(1);
        preset.setMaxPerSample(5);
        preset.setIsSystem(false);
        preset.setIsActive(true);
        Integer id = labelPresetDAO.insert(preset);
        assertNotNull(id);
        return preset;
    }

    // -----------------------------------------------------------------------
    // assertPerSamplePreset validation
    // -----------------------------------------------------------------------

    @Test
    public void assertPerSamplePreset_perSamplePreset_doesNotThrow() {
        // Should not throw for a preset with prints_per_sample=true
        testLabelPresetLinkService.assertPerSamplePreset(perSamplePreset.getId());
        // If we reach here, the method did not throw — assertion passes by absence
        assertTrue("assertPerSamplePreset should pass for per-sample preset", true);
    }

    @Test
    public void assertPerSamplePreset_orderOnlyPreset_throwsIllegalState() {
        try {
            testLabelPresetLinkService.assertPerSamplePreset(orderOnlyPreset.getId());
            fail("Expected IllegalStateException for order-only preset");
        } catch (IllegalStateException e) {
            assertTrue("Exception message should mention the preset",
                    e.getMessage().contains("order-only") || e.getMessage().contains("prints_per_sample"));
        }
    }

    @Test
    public void assertPerSamplePreset_nonExistentPreset_throwsIllegalState() {
        try {
            testLabelPresetLinkService.assertPerSamplePreset(Integer.MAX_VALUE);
            fail("Expected IllegalStateException for non-existent preset");
        } catch (IllegalStateException e) {
            assertTrue("Exception message should mention the missing id", e.getMessage().contains("does not exist")
                    || e.getMessage().contains(String.valueOf(Integer.MAX_VALUE)));
        }
    }

    // -----------------------------------------------------------------------
    // replace() — duplicate presetId rejection
    // -----------------------------------------------------------------------

    @Test
    public void replace_duplicatePresetId_throwsIllegalState() {
        TestLabelConfigForm form = new TestLabelConfigForm();
        form.setAllowOrderEntryOverride(true);
        List<TestLabelConfigForm.LinkEntry> links = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            TestLabelConfigForm.LinkEntry entry = new TestLabelConfigForm.LinkEntry();
            entry.setPresetId(perSamplePreset.getId());
            entry.setDefaultQty(1);
            entry.setMaxQty(5);
            entry.setAllowOverride(true);
            links.add(entry);
        }
        form.setLinks(links);

        try {
            testLabelConfigService.replace("1", form, TEST_SYS_USER_ID);
            fail("Expected IllegalStateException for duplicate presetId");
        } catch (IllegalStateException e) {
            assertTrue("Exception should mention duplicate", e.getMessage().toLowerCase().contains("duplicate")
                    || e.getMessage().contains(perSamplePreset.getId().toString()));
        }
    }

    @Test
    public void replace_maxQtyBelowDefaultQty_throwsIllegalState() {
        // The DB enforces max_qty >= default_qty via CHECK; without a service
        // guard that surfaces as a 500 persistence error instead of the
        // controller's intended 422. Validate up front.
        TestLabelConfigForm form = new TestLabelConfigForm();
        form.setAllowOrderEntryOverride(true);
        List<TestLabelConfigForm.LinkEntry> links = new ArrayList<>();
        TestLabelConfigForm.LinkEntry entry = new TestLabelConfigForm.LinkEntry();
        entry.setPresetId(perSamplePreset.getId());
        entry.setDefaultQty(5);
        entry.setMaxQty(2);
        entry.setAllowOverride(true);
        links.add(entry);
        form.setLinks(links);

        try {
            testLabelConfigService.replace("1", form, TEST_SYS_USER_ID);
            fail("Expected IllegalStateException for maxQty < defaultQty");
        } catch (IllegalStateException e) {
            assertTrue("Exception should mention the quantity ordering",
                    e.getMessage().toLowerCase().contains("qty") || e.getMessage().toLowerCase().contains("quantity"));
        }
    }

    // -----------------------------------------------------------------------
    // replace() — happy path + getByTestId / getLinksByTestId round-trip
    // -----------------------------------------------------------------------

    @Test
    public void replace_happyPath_persistsConfigAndLinks() {
        TestLabelConfigForm form = new TestLabelConfigForm();
        form.setAllowOrderEntryOverride(false);
        List<TestLabelConfigForm.LinkEntry> links = new ArrayList<>();
        TestLabelConfigForm.LinkEntry entry = new TestLabelConfigForm.LinkEntry();
        entry.setPresetId(perSamplePreset.getId());
        entry.setDefaultQty(2);
        entry.setMaxQty(6);
        entry.setAllowOverride(false);
        links.add(entry);
        form.setLinks(links);

        testLabelConfigService.replace("1", form, TEST_SYS_USER_ID);

        Optional<TestLabelConfig> config = testLabelConfigService.getByTestId("1");
        assertTrue("config should be persisted", config.isPresent());
        assertFalse("allowOrderEntryOverride should round-trip as false", config.get().getAllowOrderEntryOverride());

        List<TestLabelPresetLink> savedLinks = testLabelConfigService.getLinksByTestId("1");
        assertEquals("one link should be persisted", 1, savedLinks.size());
        assertEquals("presetId should round-trip", perSamplePreset.getId(), savedLinks.get(0).getPreset().getId());
        assertEquals("defaultQty should round-trip", Integer.valueOf(2), savedLinks.get(0).getDefaultQty());
        assertFalse("allowOverride should round-trip", savedLinks.get(0).getAllowOverride());
    }

    // -----------------------------------------------------------------------
    // replace() — order-only preset rejected at service level
    // -----------------------------------------------------------------------

    @Test
    public void replace_orderOnlyPreset_throwsIllegalState() {
        TestLabelConfigForm form = new TestLabelConfigForm();
        form.setAllowOrderEntryOverride(true);
        List<TestLabelConfigForm.LinkEntry> links = new ArrayList<>();
        TestLabelConfigForm.LinkEntry entry = new TestLabelConfigForm.LinkEntry();
        entry.setPresetId(orderOnlyPreset.getId());
        entry.setDefaultQty(1);
        entry.setMaxQty(5);
        entry.setAllowOverride(true);
        links.add(entry);
        form.setLinks(links);

        try {
            testLabelConfigService.replace("1", form, TEST_SYS_USER_ID);
            fail("Expected IllegalStateException for order-only preset");
        } catch (IllegalStateException e) {
            // Inversion target: if assertPerSamplePreset is removed, this catch
            // is never reached and the fail() above triggers.
            assertTrue("Exception should describe order-only violation",
                    e.getMessage().contains("order-only") || e.getMessage().contains("prints_per_sample"));
        }
    }
}
