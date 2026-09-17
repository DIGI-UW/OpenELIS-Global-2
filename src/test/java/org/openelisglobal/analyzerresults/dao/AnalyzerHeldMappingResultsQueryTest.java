package org.openelisglobal.analyzerresults.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openelisglobal.analyzerresults.valueholder.AnalyzerResults.IMPORT_ISSUE_AWAITING_SPECIMEN;
import static org.openelisglobal.analyzerresults.valueholder.AnalyzerResults.IMPORT_ISSUE_INVALID_RESULT_MAPPING;
import static org.openelisglobal.analyzerresults.valueholder.AnalyzerResults.IMPORT_ISSUE_RESULT_MAPPING_NOT_READY;
import static org.openelisglobal.analyzerresults.valueholder.AnalyzerResults.IMPORT_ISSUE_TEST_MAPPING_NOT_READY;
import static org.openelisglobal.analyzerresults.valueholder.AnalyzerResults.IMPORT_ISSUE_UNKNOWN_RESULT_VALUE;
import static org.openelisglobal.analyzerresults.valueholder.AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database query fixtures, independent of the separately tested mapping
 * lifecycle.
 */
@Transactional
public class AnalyzerHeldMappingResultsQueryTest extends BaseWebContextSensitiveTest {
    @Autowired
    private AnalyzerResultsDAO dao;
    @PersistenceContext
    private EntityManager entityManager;

    private String analyzerId;
    private String profileId;
    private String otherProfileId;
    private Set<String> mappingHolds;
    private String otherRevision;
    private String otherProfile;

    @Before
    public void createQueryFixtures() {
        Analyzer analyzer = new Analyzer();
        analyzer.setName("Held mapping query " + UUID.randomUUID());
        analyzer.ensureFhirUuid();
        analyzer.setActive(false);
        analyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
        entityManager.persist(analyzer);
        analyzerId = analyzer.getId();
        profileId = "query." + UUID.randomUUID();
        otherProfileId = "query.other." + UUID.randomUUID();
        mappingHolds = Set.of(result(profileId, 1, true, IMPORT_ISSUE_UNKNOWN_TEST),
                result(profileId, 1, true, IMPORT_ISSUE_TEST_MAPPING_NOT_READY),
                result(profileId, 1, true, IMPORT_ISSUE_UNKNOWN_RESULT_VALUE),
                result(profileId, 1, true, IMPORT_ISSUE_RESULT_MAPPING_NOT_READY),
                result(profileId, 1, true, IMPORT_ISSUE_INVALID_RESULT_MAPPING));
        otherRevision = result(profileId, 2, true, IMPORT_ISSUE_UNKNOWN_TEST);
        otherProfile = result(otherProfileId, 1, true, IMPORT_ISSUE_UNKNOWN_TEST);
        result(profileId, 1, false, IMPORT_ISSUE_UNKNOWN_TEST);
        result(profileId, 1, true, IMPORT_ISSUE_AWAITING_SPECIMEN);
        result(profileId, 1, true, null);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    public void returnsAllFiveMappingHoldsButNotEditableRowsOrOtherHoldReasons() {
        assertEquals(mappingHolds, ids(dao.findHeldMappingResultsByProfile(profileId, 1)));
    }

    @Test
    public void requiresBothTheExactSourceProfileAndRevision() {
        assertEquals(Set.of(otherRevision), ids(dao.findHeldMappingResultsByProfile(profileId, 2)));
        assertEquals(Set.of(otherProfile), ids(dao.findHeldMappingResultsByProfile(otherProfileId, 1)));
        assertTrue(dao.findHeldMappingResultsByProfile(profileId, 3).isEmpty());
        assertTrue(dao.findHeldMappingResultsByProfile("missing", 1).isEmpty());
    }

    private String result(String profile, int revision, boolean readOnly, String reason) {
        AnalyzerResults result = new AnalyzerResults();
        result.setAnalyzerId(analyzerId);
        result.setSourceProfileId(profile);
        result.setSourceProfileRevision(revision);
        result.setReadOnly(readOnly);
        result.setImportIssueReason(reason);
        result.setAccessionNumber("QUERY");
        result.setTestName("RAW-CODE");
        result.setResult("RECEIVED-VALUE");
        result.setRawTestCode("RAW-CODE");
        result.setRawResultValue("RECEIVED-VALUE");
        entityManager.persist(result);
        return result.getId();
    }

    private Set<String> ids(List<AnalyzerResults> results) {
        return results.stream().map(AnalyzerResults::getId).collect(Collectors.toSet());
    }
}
