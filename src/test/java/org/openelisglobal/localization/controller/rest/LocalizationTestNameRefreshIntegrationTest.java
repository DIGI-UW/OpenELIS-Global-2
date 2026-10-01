package org.openelisglobal.localization.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-1238 — renaming a test through its name localization refreshes the cached
 * test names the pickers, results search and reports read, as a Basic Info save
 * does. A localization that is not a test name leaves that cache alone.
 */
public class LocalizationTestNameRefreshIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95621L;
    private static final long NAME_LOCALIZATION_ID = 95622L;
    private static final long REPORTING_LOCALIZATION_ID = 95623L;
    private static final long OTHER_LOCALIZATION_ID = 95624L;
    private static final long[] LOCALIZATIONS = { NAME_LOCALIZATION_ID, REPORTING_LOCALIZATION_ID,
            OTHER_LOCALIZATION_ID };

    @Autowired
    private TestService testService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        long valueId = 95625L;
        for (long id : LOCALIZATIONS) {
            jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())", id,
                    "RefreshIT " + id);
            jdbc.update("INSERT INTO clinlims.localization_value (id, localization_id, locale, value, last_updated)"
                    + " VALUES (?, ?, 'en', ?, NOW())", valueId++, id, "RefreshIT old " + id);
        }
        jdbc.update("INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated,"
                + " name_localization_id, reporting_name_localization_id) VALUES (?, ?, ?, 'Y', ?, NOW(), ?, ?)",
                TEST_ID, "RefreshIT", "RefreshIT desc", UUID.randomUUID().toString(), NAME_LOCALIZATION_ID,
                REPORTING_LOCALIZATION_ID);
        testService.refreshTestNames();
    }

    @After
    public void tearDown() {
        cleanup();
        testService.refreshTestNames();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
        for (long id : LOCALIZATIONS) {
            jdbc.update("DELETE FROM clinlims.localization_value WHERE localization_id = ?", id);
            jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", id);
        }
    }

    private void saveEnglish(long localizationId, String value) throws Exception {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        mockMvc.perform(put("/rest/localizations/" + localizationId + "/translations")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, usd).contentType(MediaType.APPLICATION_JSON)
                .content("{\"en\":\"" + value + "\"}")).andExpect(status().isOk());
    }

    private static String cachedName() {
        return TestServiceImpl.getMap(TestServiceImpl.Entity.TEST_NAME).get(String.valueOf(TEST_ID));
    }

    @Test
    public void isNameLocalization_matchesNameAndReportingNameOnly() {
        assertTrue(testService.isNameLocalization(String.valueOf(NAME_LOCALIZATION_ID)));
        assertTrue(testService.isNameLocalization(String.valueOf(REPORTING_LOCALIZATION_ID)));
        assertFalse(testService.isNameLocalization(String.valueOf(OTHER_LOCALIZATION_ID)));
        assertFalse(testService.isNameLocalization(null));
    }

    @Test
    public void savingATestNameTranslation_refreshesTheCachedTestName() throws Exception {
        assertEquals("RefreshIT old " + NAME_LOCALIZATION_ID, cachedName());

        saveEnglish(NAME_LOCALIZATION_ID, "RefreshIT renamed");

        assertEquals("RefreshIT renamed", cachedName());
    }

    @Test
    public void savingAnUnrelatedTranslation_leavesTheTestNameCacheAlone() throws Exception {
        jdbc.update("UPDATE clinlims.localization_value SET value = ? WHERE localization_id = ?",
                "RefreshIT changed behind the cache", NAME_LOCALIZATION_ID);

        saveEnglish(OTHER_LOCALIZATION_ID, "RefreshIT other");

        assertEquals("RefreshIT old " + NAME_LOCALIZATION_ID, cachedName());
    }
}
