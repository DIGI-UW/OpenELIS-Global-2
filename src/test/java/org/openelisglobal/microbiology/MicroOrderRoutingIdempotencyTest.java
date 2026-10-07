package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.service.MicroCaseAnalysisService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroOrderRoutingService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicroOrderRoutingIdempotencyTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private MicroOrderRoutingService routingService;

    @Autowired
    private MicroCaseService caseService;

    @Autowired
    private MicroCaseAnalysisService caseAnalysisService;

    private SampleItem sampleItem;
    private String methodId;
    private TestSection labUnit;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        methodId = fixtures.createMethodId();
        sampleItem = fixtures.createSampleWithSampleItem("AMRV2IDEM");
        labUnit = fixtures.createLabUnit();
    }

    @Test
    public void repeatedRoutingDoesNotDuplicateCases() {
        var catalogTest = fixtures.createCatalogCultureTest(methodId, labUnit);
        Analysis culture = fixtures.createAnalysis(sampleItem, catalogTest);

        var first = routingService.routeAnalysesForSampleItem(sampleItem, List.of(culture), fixtures.defaultUserId());
        var second = routingService.routeAnalysesForSampleItem(sampleItem, List.of(culture), fixtures.defaultUserId());

        assertEquals(first.get(0).getId(), second.get(0).getId());
        assertEquals(1, caseService.getSiblingCases(sampleItem.getId()).size());
        assertEquals(1, caseService.getSpecimens(first.get(0).getId()).size());
        assertEquals(1, caseAnalysisService.getCaseAnalyses(first.get(0).getId()).size());
        assertEquals(culture.getId(), caseAnalysisService.getCaseAnalyses(first.get(0).getId()).get(0).getAnalysisId());
    }
}
