package org.openelisglobal.orderentry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-1424 (FR-B28): the 3.6.x.x/010 changeset gives the order-level Payment
 * status options readable English and French names in place of the raw codes
 * the generic localization backfill copied in, and leaves a name an
 * administrator changed alone. The test seeds its own rows and runs the
 * changeset's SQL, because sibling fixtures truncate the localization tables.
 */
public class PaymentStatusLabelsMigrationTest extends BaseWebContextSensitiveTest {

    private static final String CHANGESET = "liquibase/3.6.x.x/010-order-entry-cleanup.xml";

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;
    private Integer createdCategoryId;
    private final List<Integer> dictionaryIds = new ArrayList<>();
    private final List<Integer> localizationIds = new ArrayList<>();
    private final List<Integer> createdLocalizationIds = new ArrayList<>();
    private final Map<Integer, Integer> relinkedDictionaries = new java.util.HashMap<>();
    private final List<List<Map<String, Object>>> savedValues = new ArrayList<>();

    @Before
    public void seedPaymentOptions() {
        jdbc = new JdbcTemplate(dataSource);
        List<Integer> categories = jdbc.queryForList(
                "SELECT id FROM clinlims.dictionary_category WHERE name = 'patientPayment'", Integer.class);
        Integer categoryId;
        if (categories.isEmpty()) {
            categoryId = jdbc.queryForObject("SELECT nextval('clinlims.dictionary_category_seq')", Integer.class);
            jdbc.update("INSERT INTO clinlims.dictionary_category (id, name, description, lastupdated)"
                    + " VALUES (?, 'patientPayment', 'patient payment', now())", categoryId);
            createdCategoryId = categoryId;
        } else {
            categoryId = categories.get(0);
        }
        seedOption(categoryId, "reducedInsurance", "reducedInsurance");
        seedOption(categoryId, "paidInFull", "Settled at the counter");
    }

    @After
    public void removeSeededRows() {
        for (Integer id : localizationIds) {
            jdbc.update("DELETE FROM clinlims.localization_value WHERE localization_id = ?", id);
        }
        for (List<Map<String, Object>> rows : savedValues) {
            for (Map<String, Object> row : rows) {
                jdbc.update(
                        "INSERT INTO clinlims.localization_value (id, localization_id, locale, value, last_updated)"
                                + " VALUES (?, ?, ?, ?, ?)",
                        row.get("id"), row.get("localization_id"), row.get("locale"), row.get("value"),
                        row.get("last_updated"));
            }
        }
        for (Integer id : dictionaryIds) {
            jdbc.update("DELETE FROM clinlims.dictionary WHERE id = ?", id);
        }
        relinkedDictionaries.forEach((dictionaryId, previous) -> jdbc.update(
                "UPDATE clinlims.dictionary SET name_localization_id = ? WHERE id = ?", previous, dictionaryId));
        for (Integer id : createdLocalizationIds) {
            jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", id);
        }
        if (createdCategoryId != null) {
            jdbc.update("DELETE FROM clinlims.dictionary_category WHERE id = ?", createdCategoryId);
        }
    }

    @Test
    public void aBackfilledCodeBecomesItsLabelInEnglishAndFrench() throws Exception {
        runChangeset();

        assertEquals("Reduced insurance payment", name(localizationIds.get(0), "en"));
        assertEquals("Tarif reduit payment assurance", name(localizationIds.get(0), "fr"));
    }

    @Test
    public void aNameAnAdministratorChangedIsKept() throws Exception {
        runChangeset();

        assertEquals("Settled at the counter", name(localizationIds.get(1), "en"));
    }

    @Test
    public void runningItAgainChangesNothing() throws Exception {
        runChangeset();
        runChangeset();

        assertEquals(Integer.valueOf(1),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.localization_value WHERE localization_id = ? AND locale = 'fr'",
                        Integer.class, localizationIds.get(0)));
    }

    private void seedOption(Integer categoryId, String code, String englishName) {
        List<Map<String, Object>> existing = jdbc.queryForList(
                "SELECT id, name_localization_id FROM clinlims.dictionary WHERE dictionary_category_id = ?"
                        + " AND dict_entry = ?",
                categoryId, code);
        Integer localizationId;
        if (existing.isEmpty()) {
            localizationId = newLocalization();
            Integer dictionaryId = jdbc.queryForObject("SELECT nextval('clinlims.dictionary_seq')", Integer.class);
            jdbc.update(
                    "INSERT INTO clinlims.dictionary (id, is_active, dict_entry, lastupdated,"
                            + " dictionary_category_id, name_localization_id) VALUES (?, 'Y', ?, now(), ?, ?)",
                    dictionaryId, code, categoryId, localizationId);
            dictionaryIds.add(dictionaryId);
        } else {
            Integer dictionaryId = ((Number) existing.get(0).get("id")).intValue();
            Object linked = existing.get(0).get("name_localization_id");
            Integer linkedId = linked == null ? null : ((Number) linked).intValue();
            boolean localizationPresent = linkedId != null
                    && jdbc.queryForObject("SELECT count(*) FROM clinlims.localization WHERE id = ?", Integer.class,
                            linkedId) > 0;
            if (localizationPresent) {
                localizationId = linkedId;
                savedValues.add(jdbc.queryForList(
                        "SELECT id, localization_id, locale, value, last_updated FROM clinlims.localization_value"
                                + " WHERE localization_id = ?",
                        localizationId));
            } else {
                localizationId = newLocalization();
                relinkedDictionaries.put(dictionaryId, linkedId);
                jdbc.update("UPDATE clinlims.dictionary SET name_localization_id = ? WHERE id = ?", localizationId,
                        dictionaryId);
            }
        }
        jdbc.update("DELETE FROM clinlims.localization_value WHERE localization_id = ?", localizationId);
        jdbc.update(
                "INSERT INTO clinlims.localization_value (id, localization_id, locale, value, last_updated)"
                        + " VALUES (nextval('clinlims.localization_value_seq'), ?, 'en', ?, now())",
                localizationId, englishName);
        localizationIds.add(localizationId);
    }

    private Integer newLocalization() {
        Integer id = jdbc.queryForObject("SELECT nextval('clinlims.localization_seq')", Integer.class);
        jdbc.update(
                "INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, 'dictionary name', now())",
                id);
        createdLocalizationIds.add(id);
        return id;
    }

    private String name(Integer localizationId, String locale) {
        List<String> values = jdbc.queryForList(
                "SELECT value FROM clinlims.localization_value WHERE localization_id = ? AND locale = ?", String.class,
                localizationId, locale);
        return values.isEmpty() ? null : values.get(0);
    }

    private void runChangeset() throws Exception {
        String xml;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(CHANGESET)) {
            assertNotNull(CHANGESET + " is on the classpath", in);
            xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher matcher = Pattern
                .compile("id=\"3\\.6\\.0\\.0-payment-status-labels\".*?<sql[^>]*>(.*?)</sql>", Pattern.DOTALL)
                .matcher(xml);
        assertEquals("the payment labels changeset carries its SQL", true, matcher.find());
        jdbc.execute(matcher.group(1).replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&"));
    }
}
