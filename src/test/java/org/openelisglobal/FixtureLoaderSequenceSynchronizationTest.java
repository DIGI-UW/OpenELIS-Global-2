package org.openelisglobal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openelisglobal.analyzer.AnalyzerTestProfileCatalog.PROFILE_ID;
import static org.openelisglobal.analyzer.AnalyzerTestProfileCatalog.PROFILE_REVISION;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.junit.Test;
import org.openelisglobal.analyzer.service.AnalyzerProfileBindingService;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Validates actual sequence allocation, including repeated fixture loading. */
@Transactional
public class FixtureLoaderSequenceSynchronizationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private AnalyzerService analyzers;
    @Autowired
    private AnalyzerProfileBindingService profiles;
    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void analyzerFixtureAllowsRealProfileMappingAndAnalyzerCreationWithoutIdCollisions() throws Exception {
        executeDataSetWithStateManagement("testdata/facade-device.xml");
        Analyzer analyzer = new Analyzer();
        analyzer.ensureFhirUuid();
        analyzer.setName("Sequence regression " + UUID.randomUUID());
        analyzer.setSysUserId(TEST_SYS_USER_ID);
        profiles.assignProfile(analyzer, PROFILE_ID, PROFILE_REVISION, TEST_SYS_USER_ID);
        String id = analyzers.insert(analyzer);
        entityManager.flush();
        entityManager.clear();

        Analyzer saved = analyzers.get(id);
        assertEquals(analyzer.getName(), saved.getName());
        assertEquals(PROFILE_ID, saved.getPinnedProfileBinding().getProfileId());
        assertTrue(Long.parseLong(id) > 3);
        assertTrue(Long.parseLong(saved.getPinnedProfileBinding().getId()) > 3);
        assertTrue(Long.parseLong(saved.getSiteBindingRevision().getId()) > 3);
        assertTrue(Long.parseLong(saved.getSiteBindingRevision().getSiteBinding().getId()) > 3);
        assertEquals("Cobas 6800", analyzers.get("1").getName());
        assertEquals(Integer.valueOf(4), jdbcTemplate.queryForObject("SELECT COUNT(*) FROM analyzer", Integer.class));
    }

    @Test
    public void fixtureAdvancesPastImportedIdsWithoutReusingPreviouslyAllocatedIds() throws Exception {
        long previousNext = jdbcTemplate.queryForObject(
                "SELECT last_value + CASE WHEN is_called THEN 1 ELSE 0 END FROM clinlims.observation_history_seq",
                Long.class);
        executeDataSetWithStateManagement("testdata/observation-history.xml");
        long importedNext = jdbcTemplate
                .queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM clinlims.observation_history", Long.class);
        long allocated = jdbcTemplate.queryForObject("SELECT nextval('clinlims.observation_history_seq')", Long.class);
        assertEquals("Loading a fixture must avoid both imported IDs and already allocated IDs",
                Math.max(previousNext, importedNext), allocated);

        executeDataSetWithStateManagement("testdata/observation-history.xml");
        assertEquals("Reloading the same small fixture must not reallocate the previous ID",
                Long.valueOf(allocated + 1),
                jdbcTemplate.queryForObject("SELECT nextval('clinlims.observation_history_seq')", Long.class));
    }
}
