package org.openelisglobal.test;

import static org.junit.Assert.assertEquals;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationValueService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * A test's name is shown in the reader's language, but reading a test must
 * never write it back (OGC-1442). Before the fix a user working in French
 * turned every read of a test into an update of its stored name, so the shared
 * name column switched language with whoever read it last and two concurrent
 * reads failed with a stale-row error.
 */
@Transactional
public class TestNameLocaleReadTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestService testService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private LocalizationValueService localizationValueService;

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    private JdbcTemplate jdbc;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test.xml");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("update clinlims.test set name_localization_id = 3 where id = 1");
        jdbc.update("update clinlims.localization set lastupdated = now() where id = 3");
        jdbc.update("update clinlims.localization_value set last_updated = now() where localization_id = 3");
    }

    @After
    public void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    public void readingATestInFrenchShowsTheFrenchNameButLeavesTheStoredRowAlone() {
        Timestamp lastUpdatedBefore = storedLastUpdated();
        LocaleContextHolder.setLocale(Locale.FRENCH);

        org.openelisglobal.test.valueholder.Test test = testService.get("1");
        entityManager.flush();

        assertEquals("Merci", test.getName());
        assertEquals("Complete Blood Count", storedName());
        assertEquals(lastUpdatedBefore, storedLastUpdated());
    }

    @Test
    public void readingTestsByLabUnitInFrenchLeavesTheStoredRowsAlone() {
        LocaleContextHolder.setLocale(Locale.FRENCH);

        testService.getTestsByTestSectionIds(List.of("1", "2"));
        entityManager.flush();

        assertEquals("Complete Blood Count", storedName());
    }

    @Test
    public void renamingATestUpdatesItsStoredNameWithTheEnglishName() {
        LocaleContextHolder.setLocale(Locale.FRENCH);
        Localization name = localizationService.get("3");
        name.setLocalizedValue(Locale.ENGLISH, "Blood Count Renamed");
        name.setLocalizedValue(Locale.FRENCH, "Numération renommée");

        localizationService.update(name);
        entityManager.flush();

        assertEquals("Blood Count Renamed", storedName());
    }

    @Test
    public void editingATranslationKeepsTheStoredNameOnTheEnglishName() {
        localizationValueService.setTranslation("3", "fr", "Seulement en français", "1");
        entityManager.flush();
        assertEquals("blood test", storedName());

        localizationValueService.setTranslation("3", "en", "Blood Count From The Editor", "1");
        entityManager.flush();
        assertEquals("Blood Count From The Editor", storedName());
    }

    private String storedName() {
        return jdbc.queryForObject("select name from clinlims.test where id = 1", String.class);
    }

    private Timestamp storedLastUpdated() {
        return jdbc.queryForObject("select lastupdated from clinlims.test where id = 1", Timestamp.class);
    }
}
