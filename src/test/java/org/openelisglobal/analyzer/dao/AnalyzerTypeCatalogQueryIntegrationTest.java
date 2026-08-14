package org.openelisglobal.analyzer.dao;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTestPK;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AnalyzerTypeCatalogQueryIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String PROFILE_A = "site.catalog-query-a";
    private static final String PROFILE_B = "site.catalog-query-b";

    @Autowired
    private AnalyzerDAO analyzerDAO;

    @Autowired
    private AnalyzerSiteBindingDAO bindingDAO;

    @Autowired
    private AnalyzerSiteBindingRevisionDAO revisionDAO;

    @Autowired
    private AnalyzerSiteBindingTestDAO testDAO;

    @Test
    public void batchQueriesReturnRevisionRowsAndAnalyzerUsageCounts() {
        AnalyzerSiteBindingRevision revisionA = persistBinding(PROFILE_A);
        AnalyzerSiteBindingRevision revisionB = persistBinding(PROFILE_B);
        testDAO.insert(row(revisionA, "wbc"));
        testDAO.insert(row(revisionA, "hgb"));
        testDAO.insert(row(revisionB, "glucose"));
        analyzerDAO.insert(analyzer("OGC-1054 catalog query A1", revisionA));
        analyzerDAO.insert(analyzer("OGC-1054 catalog query A2", revisionA));
        analyzerDAO.insert(analyzer("OGC-1054 catalog query B1", revisionB));

        List<AnalyzerSiteBindingTest> rows = testDAO.findByRevisionIds(List.of(revisionB.getId(), revisionA.getId()));
        Map<String, Long> counts = analyzerDAO
                .countByBridgeProfileIds(List.of(PROFILE_A, PROFILE_B, "site.catalog-query-unused"));
        List<String> revisionASourceRows = rows.stream()
                .filter(row -> revisionA.getId().equals(row.getId().getSiteBindingRevisionId()))
                .map(row -> row.getId().getSourceRowKey()).toList();
        List<String> revisionBSourceRows = rows.stream()
                .filter(row -> revisionB.getId().equals(row.getId().getSiteBindingRevisionId()))
                .map(row -> row.getId().getSourceRowKey()).toList();

        assertEquals(3, rows.size());
        assertEquals(List.of("hgb", "wbc"), revisionASourceRows);
        assertEquals(List.of("glucose"), revisionBSourceRows);
        assertEquals(Map.of(PROFILE_A, 2L, PROFILE_B, 1L), counts);
    }

    private AnalyzerSiteBindingRevision persistBinding(String profileId) {
        Timestamp now = Timestamp.from(Instant.parse("2026-08-14T03:00:00Z"));
        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setId(UUID.randomUUID().toString());
        binding.setCreatedBy("1");
        binding.setCreatedAt(now);
        bindingDAO.insert(binding);

        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setId(UUID.randomUUID().toString());
        revision.setSiteBinding(binding);
        revision.setRevisionNumber(1);
        revision.setBridgeProfileId(profileId);
        revision.setBridgeProfileRevision(1);
        revision.setFingerprint("sha256:" + (PROFILE_A.equals(profileId) ? "a" : "b").repeat(64));
        revision.setCreatedBy("1");
        revision.setCreatedAt(now);
        revisionDAO.insert(revision);
        return revision;
    }

    private static AnalyzerSiteBindingTest row(AnalyzerSiteBindingRevision revision, String sourceRowKey) {
        AnalyzerSiteBindingTest row = new AnalyzerSiteBindingTest();
        row.setId(new AnalyzerSiteBindingTestPK(revision.getId(), sourceRowKey));
        row.setSiteBindingRevision(revision);
        row.setRawAnalyzerCode(sourceRowKey.toUpperCase());
        row.setMappingState(AnalyzerSiteBindingTest.MappingState.UNRESOLVED);
        return row;
    }

    private static Analyzer analyzer(String name, AnalyzerSiteBindingRevision revision) {
        Analyzer analyzer = new Analyzer();
        analyzer.setName(name + " " + UUID.randomUUID());
        analyzer.setBridgeProfileId(revision.getBridgeProfileId());
        analyzer.setBridgeProfileRevision(revision.getBridgeProfileRevision());
        analyzer.setSiteBindingRevision(revision);
        analyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
        return analyzer;
    }
}
