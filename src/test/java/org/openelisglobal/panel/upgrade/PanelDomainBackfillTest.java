package org.openelisglobal.panel.upgrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.sql.Connection;
import java.util.UUID;
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
 * OGC-1599: a database upgraded from before OGC-1209 keeps the CLINICAL default
 * on every panel, so vector and environmental panels never show under their
 * domain. The rows below are shaped the way such a database holds them: the
 * sample types and tests carry their domain, the panel does not.
 */
@Transactional
public class PanelDomainBackfillTest extends BaseWebContextSensitiveTest {

    private static final String BASE_CHANGELOG = "liquibase/3.6.x.x/base.xml";
    private static final String CHANGESET = "liquibase/3.6.x.x/025-panel-domain-from-members.xml";

    private static final long MOSQUITO = 99610L;
    private static final long FLEA = 99611L;
    private static final long WATER = 99612L;
    private static final long BLOOD = 99613L;
    private static final long LEGACY_FLY = 99614L;

    private static final long MOSQUITO_TEST = 99620L;
    private static final long FLEA_TEST = 99621L;
    private static final long WATER_TEST = 99622L;
    private static final long BLOOD_TEST = 99623L;
    private static final long NO_SPECIMEN_TEST = 99624L;
    private static final long FLY_TEST = 99625L;
    private static final long CLINICAL_TEST_ON_MOSQUITO = 99626L;
    private static final long VECTOR_TEST_ON_BLOOD = 99627L;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;
    private long nextId = 99700L;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        sampleType(MOSQUITO, "Mosquito 1599", "VECTOR");
        sampleType(FLEA, "Flea 1599", "VECTOR");
        sampleType(WATER, "Water 1599", "ENVIRONMENTAL");
        sampleType(BLOOD, "Blood 1599", "CLINICAL");
        sampleType(LEGACY_FLY, "Fly 1599", "A");
        test(MOSQUITO_TEST, "Mosquito ID 1599", "VECTOR", MOSQUITO);
        test(FLEA_TEST, "Flea ID 1599", "VECTOR", FLEA);
        test(WATER_TEST, "Coliforms 1599", "ENVIRONMENTAL", WATER);
        test(BLOOD_TEST, "Hemoglobin 1599", "CLINICAL", BLOOD);
        test(NO_SPECIMEN_TEST, "No specimen 1599", "VECTOR");
        test(FLY_TEST, "Fly ID 1599", "VECTOR", LEGACY_FLY);
        test(CLINICAL_TEST_ON_MOSQUITO, "Clinical on mosquito 1599", "CLINICAL", MOSQUITO);
        test(VECTOR_TEST_ON_BLOOD, "Vector on blood 1599", "VECTOR", BLOOD);
    }

    @Test
    public void aPanelWhoseMembersAreAllVectorMovesToVector() throws Exception {
        long panel = panel("Vector panel 1599", "CLINICAL", MOSQUITO_TEST, FLEA_TEST);

        run();

        assertEquals("VECTOR", domainOf(panel));
    }

    @Test
    public void aPanelWhoseMembersAreAllEnvironmentalMovesToEnvironmental() throws Exception {
        long panel = panel("Water panel 1599", "CLINICAL", WATER_TEST);

        run();

        assertEquals("ENVIRONMENTAL", domainOf(panel));
    }

    @Test
    public void aLegacyOneLetterSampleTypeDomainCountsAsItsDomain() throws Exception {
        long panel = panel("Fly panel 1599", "CLINICAL", FLY_TEST, MOSQUITO_TEST);

        run();

        assertEquals("VECTOR", domainOf(panel));
    }

    @Test
    public void aPanelWhoseSampleTypesSpanDomainsKeepsItsDomainAndIsReported() throws Exception {
        long panel = panel("Mixed panel 1599", "CLINICAL", MOSQUITO_TEST, WATER_TEST);

        PanelDomainBackfill.Outcome outcome = run();

        assertEquals("CLINICAL", domainOf(panel));
        assertTrue(outcome.kept().toString(), keptReason(outcome, "Mixed panel 1599").contains("several domains"));
    }

    @Test
    public void aPanelWithoutSampleTypesKeepsItsDomainAndIsReported() throws Exception {
        long panel = panel("No specimen panel 1599", "CLINICAL", NO_SPECIMEN_TEST);
        long empty = panel("Empty panel 1599", "CLINICAL");

        PanelDomainBackfill.Outcome outcome = run();

        assertEquals("CLINICAL", domainOf(panel));
        assertEquals("CLINICAL", domainOf(empty));
        assertTrue(keptReason(outcome, "No specimen panel 1599").contains("no sample types"));
        assertTrue(keptReason(outcome, "Empty panel 1599").contains("no tests"));
    }

    /**
     * The panel save guard refuses a domain its member tests do not share, so a
     * panel whose vector specimens sit under a clinical test stays where the guard
     * keeps it editable.
     */
    @Test
    public void aPanelWhoseTestsAreInAnotherDomainThanItsSampleTypesKeepsItsDomainAndIsReported() throws Exception {
        long panel = panel("Clinical test panel 1599", "CLINICAL", CLINICAL_TEST_ON_MOSQUITO, MOSQUITO_TEST);

        PanelDomainBackfill.Outcome outcome = run();

        assertEquals("CLINICAL", domainOf(panel));
        assertTrue(keptReason(outcome, "Clinical test panel 1599").contains("member tests"));
    }

    @Test
    public void aPanelOnClinicalSampleTypesWhoseTestsAreInAnotherDomainIsReported() throws Exception {
        long panel = panel("Vector test on blood panel 1599", "CLINICAL", VECTOR_TEST_ON_BLOOD);

        PanelDomainBackfill.Outcome outcome = run();

        assertEquals("CLINICAL", domainOf(panel));
        assertTrue(outcome.kept().toString(),
                keptReason(outcome, "Vector test on blood panel 1599").contains("member tests are (VECTOR)"));
    }

    @Test
    public void aClinicalPanelIsLeftAloneAndNotReported() throws Exception {
        long panel = panel("Blood panel 1599", "CLINICAL", BLOOD_TEST);

        PanelDomainBackfill.Outcome outcome = run();

        assertEquals("CLINICAL", domainOf(panel));
        assertEquals("", keptReason(outcome, "Blood panel 1599"));
    }

    @Test
    public void aDomainAlreadySetIsNeverChanged() throws Exception {
        long panel = panel("Set vector panel 1599", "ENVIRONMENTAL", MOSQUITO_TEST);

        run();

        assertEquals("ENVIRONMENTAL", domainOf(panel));
    }

    @Test
    public void runningItTwiceChangesNothingMore() throws Exception {
        long vector = panel("Vector panel 1599", "CLINICAL", MOSQUITO_TEST);
        long mixed = panel("Mixed panel 1599", "CLINICAL", MOSQUITO_TEST, WATER_TEST);
        run();

        PanelDomainBackfill.Outcome second = run();

        assertEquals("VECTOR", domainOf(vector));
        assertEquals("CLINICAL", domainOf(mixed));
        assertTrue(second.moved().toString(), second.moved().keySet().stream().noneMatch(n -> n.endsWith("1599")));
    }

    @Test
    public void theChangesetRunsTheBackfillFromTheVersionChangelog() throws Exception {
        Document base = parse(BASE_CHANGELOG);
        boolean included = false;
        for (int i = 0; i < base.getElementsByTagName("include").getLength(); i++) {
            included |= ((Element) base.getElementsByTagName("include").item(i)).getAttribute("file")
                    .equals("025-panel-domain-from-members.xml");
        }
        assertTrue(included);
        Element change = (Element) parse(CHANGESET).getElementsByTagName("customChange").item(0);
        assertEquals(PanelDomainBackfill.class.getName(), change.getAttribute("class"));
    }

    private PanelDomainBackfill.Outcome run() throws Exception {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        return PanelDomainBackfill.apply(connection);
    }

    private String keptReason(PanelDomainBackfill.Outcome outcome, String panelName) {
        return outcome.kept().stream().filter(k -> k.panel().equals(panelName)).map(PanelDomainBackfill.Kept::reason)
                .findFirst().orElse("");
    }

    private String domainOf(long panelId) {
        return jdbc.queryForObject("SELECT domain FROM clinlims.panel WHERE id = ?", String.class, panelId);
    }

    private void sampleType(long id, String description, String domain) {
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())", id,
                description);
        jdbc.update(
                "INSERT INTO clinlims.type_of_sample (id, description, domain, local_abbrev, is_active, sort_order,"
                        + " name_localization_id, lastupdated) VALUES (?, ?, ?, ?, 'true', ?, ?, NOW())",
                id, description, domain, "T" + id % 1000, id, id);
    }

    private void test(long id, String name, String domain, long... sampleTypes) {
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, orderable, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, ?, true, NOW())",
                id, name, name, UUID.randomUUID().toString(), domain);
        for (long sampleType : sampleTypes) {
            jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id, is_panel)"
                    + " VALUES (nextval('sample_type_test_seq'), ?, ?, 'false')", sampleType, id);
        }
    }

    private long panel(String name, String domain, long... tests) {
        long id = nextId++;
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())", id, name);
        jdbc.update("INSERT INTO clinlims.panel (id, name, description, is_active, name_localization_id, domain)"
                + " VALUES (?, ?, ?, 'Y', ?, ?)", id, name, name, id, domain);
        for (long test : tests) {
            jdbc.update("INSERT INTO clinlims.panel_item (id, panel_id, test_id) VALUES (?, ?, ?)", nextId++, id, test);
        }
        return id;
    }

    private static Document parse(String resource) throws Exception {
        try (InputStream xml = PanelDomainBackfillTest.class.getClassLoader().getResourceAsStream(resource)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(xml);
        }
    }
}
