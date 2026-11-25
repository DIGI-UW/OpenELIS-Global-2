package org.openelisglobal.siteinformation.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-1364 v3: a setting saved from a page that loaded an older version is
 * recognised as changed since, so the save is refused instead of overwriting
 * the newer value.
 */
public class SiteInformationChangedSinceTest extends BaseWebContextSensitiveTest {

    private static final String SETTING = "ogc1364ChangedSinceTest";

    @Autowired
    private SiteInformationService siteInformationService;

    private String id;
    private long loaded;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        id = jdbcTemplate.queryForObject("INSERT INTO clinlims.site_information (id, name, lastupdated, value,"
                + " value_type) VALUES (nextval('clinlims.site_information_seq'), ?, now(), 'false', 'boolean')"
                + " RETURNING id::text", String.class, SETTING);
        SiteInformation stored = siteInformationService.get(id);
        loaded = stored.getLastupdated().getTime();
    }

    @After
    public void removeSetting() {
        jdbcTemplate.update("DELETE FROM clinlims.site_information WHERE name = ?", SETTING);
    }

    @Test
    public void theVersionThePageLoadedIsNotChanged() {
        assertFalse(siteInformationService.changedSince(id, loaded));
    }

    @Test
    public void anOlderVersionIsChangedSince() {
        assertTrue(siteInformationService.changedSince(id, loaded - 60_000));
    }

    @Test
    public void aSaveFromAnotherPageInBetweenMakesTheLoadedVersionStale() {
        jdbcTemplate.update("UPDATE clinlims.site_information SET lastupdated = lastupdated + interval '1 minute'"
                + " WHERE id = ?", Integer.valueOf(id));
        assertTrue(siteInformationService.changedSince(id, loaded));
    }

    @Test
    public void anUnknownSettingIsNotChanged() {
        assertFalse(siteInformationService.changedSince("987654321", loaded));
    }

    @Test
    public void aFormWithoutAVersionIsNotChecked() {
        assertFalse(siteInformationService.changedSince(id, null));
    }
}
