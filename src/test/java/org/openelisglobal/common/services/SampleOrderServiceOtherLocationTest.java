package org.openelisglobal.common.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * The sampling location list ends with an "Other" entry that asks for a typed
 * location. Its id differs between sites, and the order screens guessed it (a
 * hard-coded 1310, or an extra "Other" choice that stored nothing), so the "If
 * other, specify" field never opened. The server now names the entry by its
 * local abbreviation.
 */
@Transactional
public class SampleOrderServiceOtherLocationTest extends BaseWebContextSensitiveTest {

    private long categoryId;

    /**
     * Other suites replace the dictionary categories, so the sampling location
     * category is created here when it is missing.
     */
    @Before
    public void findCategory() {
        jdbcTemplate
                .update("INSERT INTO clinlims.dictionary_category (id, description, local_abbrev, name, lastupdated)"
                        + " SELECT 990001, 'Test Location Codes', 'TL1515', 'testLocationCode', now()"
                        + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.dictionary_category WHERE name = 'testLocationCode')");
        categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM clinlims.dictionary_category WHERE name = 'testLocationCode'", Long.class);
        jdbcTemplate.update("DELETE FROM clinlims.dictionary WHERE dictionary_category_id = ? AND id >= 990001",
                categoryId);
    }

    private void addEntry(long id, String entry, String abbreviation) {
        jdbcTemplate.update(
                "INSERT INTO clinlims.dictionary (id, is_active, dict_entry, local_abbrev, dictionary_category_id,"
                        + " lastupdated) VALUES (?, 'Y', ?, ?, ?, now())",
                id, entry, abbreviation, categoryId);
    }

    @Test
    public void namesTheOtherEntryOfTheSamplingLocationList() {
        jdbcTemplate.update(
                "UPDATE clinlims.dictionary SET local_abbrev = local_abbrev || '-seed' WHERE dictionary_category_id = ?"
                        + " AND lower(local_abbrev) = 'other'",
                categoryId);
        addEntry(990001, "Lab test site 1515", "B1515");
        addEntry(990002, "Autre lieu 1515", "Other");

        assertEquals("990002", SampleOrderService.otherTestLocationCodeId());
    }

    @Test
    public void answersNullWhenTheListHasNoOtherEntry() {
        jdbcTemplate.update(
                "UPDATE clinlims.dictionary SET local_abbrev = local_abbrev || '-seed' WHERE dictionary_category_id = ?"
                        + " AND lower(local_abbrev) = 'other'",
                categoryId);
        addEntry(990001, "Lab test site 1515", "B1515");

        assertNull(SampleOrderService.otherTestLocationCodeId());
    }
}
