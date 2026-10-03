package org.openelisglobal.analyzer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.openelisglobal.analyzer.AnalyzerTestProfileCatalog.PROFILE_ID;
import static org.openelisglobal.analyzer.AnalyzerTestProfileCatalog.PROFILE_REVISION;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerProfileBindingService;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AnalyzerServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    private AnalyzerService analyzerService;
    @Autowired
    private AnalyzerProfileBindingService profiles;
    @PersistenceContext
    private EntityManager entityManager;

    private Set<String> expectedIds;
    private String firstAnalyzerId;
    private String firstAnalyzerName;

    @Before
    public void createOwnedAnalyzers() {
        expectedIds = analyzerService.getAll().stream().map(Analyzer::getId)
                .collect(Collectors.toCollection(HashSet::new));
        String prefix = UUID.randomUUID().toString().substring(0, 12);
        for (String model : new String[] { "Cobas 6800", "ABL800 FLEX", "Sysmex XN-1000" }) {
            Analyzer analyzer = new Analyzer();
            analyzer.ensureFhirUuid();
            analyzer.setName(prefix + " " + model);
            analyzer.setActive(false);
            analyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
            analyzer.setSysUserId(TEST_SYS_USER_ID);
            profiles.assignProfile(analyzer, PROFILE_ID, PROFILE_REVISION, TEST_SYS_USER_ID);
            String id = analyzerService.insert(analyzer);
            expectedIds.add(id);
            if (firstAnalyzerId == null) {
                firstAnalyzerId = id;
                firstAnalyzerName = analyzer.getName();
            }
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    public void getAnalyzersFromDatabaseReturnsOwnedAndExistingRecords() {
        assertEquals(expectedIds, analyzerService.getAll().stream().map(Analyzer::getId).collect(Collectors.toSet()));
    }

    @Test
    public void getAnalyzerByNameReturnsThePersistedAnalyzer() {
        Analyzer analyzer = analyzerService.getAnalyzerByName(firstAnalyzerName);
        assertNotNull(analyzer);
        assertEquals(firstAnalyzerId, analyzer.getId());
        assertEquals(firstAnalyzerName, analyzer.getName());
    }

    @Test
    public void getAnalyzerByNameReturnsNullForAnUnknownName() {
        assertNull(analyzerService.getAnalyzerByName("Missing analyzer " + UUID.randomUUID()));
    }
}
