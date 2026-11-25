package org.openelisglobal.observationhistory.upgrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.sql.Connection;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * OGC-1669: an order's request and next visit dates were kept in the site's
 * date format of the day they were saved; the upgrade stores them as ISO dates,
 * read in the site's current format.
 */
@Transactional
public class ObservationDatesToIsoTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;
    private long requestDate;
    private long nextVisitDate;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/observation-history.xml");
        jdbc = new JdbcTemplate(dataSource);
        requestDate = type("requestDate", 99851L);
        nextVisitDate = type("nextVisitDate", 99852L);
    }

    @Test
    public void aDateInTheSitesFormatIsStoredAsIso() throws Exception {
        long us = observation(99861L, requestDate, "11/12/2026");
        long visit = observation(99862L, nextVisitDate, "01/31/2027");

        ObservationDatesToIso.Outcome outcome = run(Locale.forLanguageTag("en-US"));

        assertEquals("2026-11-12", valueOf(us));
        assertEquals("2027-01-31", valueOf(visit));
        assertEquals(2, outcome.converted());
    }

    @Test
    public void aDayOrMonthWrittenWithOneDigitIsConverted() throws Exception {
        long unpadded = observation(99870L, requestDate, "1/5/2026");

        run(Locale.forLanguageTag("en-US"));

        assertEquals("2026-01-05", valueOf(unpadded));
    }

    @Test
    public void aFrenchSiteReadsTheDayFirst() throws Exception {
        long french = observation(99863L, requestDate, "11/12/2026");

        run(Locale.forLanguageTag("fr-FR"));

        assertEquals("2026-12-11", valueOf(french));
    }

    /**
     * A site that changed its format before the upgrade holds older dates in the
     * other order: one that reads only that way is converted, one that reads both
     * ways follows the site's own order.
     */
    @Test
    public void aDateSavedBeforeTheSiteChangedItsFormatIsReadTheOtherWayWhenOnlyThatWorks() throws Exception {
        long usOnly = observation(99868L, requestDate, "11/20/2026");
        long either = observation(99869L, nextVisitDate, "11/12/2026");

        run(Locale.forLanguageTag("fr-FR"));

        assertEquals("2026-11-20", valueOf(usOnly));
        assertEquals("2026-12-11", valueOf(either));
    }

    @Test
    public void anIsoDateAnUnreadableValueAndOtherObservationsAreLeftAlone() throws Exception {
        long iso = observation(99864L, requestDate, "2026-10-01");
        long unreadable = observation(99865L, requestDate, "31/31/2026");
        long occupation = observation(99866L, type("occupation1669", 99853L), "11/12/2026");

        ObservationDatesToIso.Outcome outcome = run(Locale.forLanguageTag("en-US"));

        assertEquals("2026-10-01", valueOf(iso));
        assertEquals("31/31/2026", valueOf(unreadable));
        assertEquals("11/12/2026", valueOf(occupation));
        assertEquals(1, outcome.alreadyIso());
        assertTrue(outcome.unreadable().toString(),
                outcome.unreadable().stream().anyMatch(value -> value.contains("99865")));
    }

    @Test
    public void runningItAgainChangesNothing() throws Exception {
        long us = observation(99867L, requestDate, "11/12/2026");
        run(Locale.forLanguageTag("en-US"));

        ObservationDatesToIso.Outcome again = run(Locale.forLanguageTag("fr-FR"));

        assertEquals("2026-11-12", valueOf(us));
        assertEquals(0, again.converted());
    }

    @Test
    public void theChangesetRunsFromTheVersionChangelog() throws Exception {
        Document base = parse("liquibase/3.6.x.x/base.xml");
        boolean included = false;
        for (int i = 0; i < base.getElementsByTagName("include").getLength(); i++) {
            included |= ((Element) base.getElementsByTagName("include").item(i)).getAttribute("file")
                    .equals("042-observation-dates-to-iso.xml");
        }
        assertTrue(included);
        Element change = (Element) parse("liquibase/3.6.x.x/042-observation-dates-to-iso.xml")
                .getElementsByTagName("customChange").item(0);
        assertEquals(ObservationDatesToIso.class.getName(), change.getAttribute("class"));
    }

    private ObservationDatesToIso.Outcome run(Locale locale) throws Exception {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        return ObservationDatesToIso.apply(connection, locale);
    }

    private long type(String name, long id) {
        List<Long> ids = jdbc.queryForList("SELECT id FROM clinlims.observation_history_type WHERE type_name = ?",
                Long.class, name);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        jdbc.update("INSERT INTO clinlims.observation_history_type (id, type_name, description, lastupdated)"
                + " VALUES (?, ?, ?, now())", id, name, name);
        return id;
    }

    private long observation(long id, long type, String value) {
        jdbc.update("INSERT INTO clinlims.observation_history (id, sample_id, observation_history_type_id,"
                + " value_type, value, lastupdated) VALUES (?, 1, ?, 'L', ?, now())", id, type, value);
        return id;
    }

    private String valueOf(long id) {
        return jdbc.queryForObject("SELECT value FROM clinlims.observation_history WHERE id = ?", String.class, id);
    }

    private static Document parse(String resource) throws Exception {
        try (InputStream in = ObservationDatesToIsoTest.class.getClassLoader().getResourceAsStream(resource)) {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
        }
    }
}
