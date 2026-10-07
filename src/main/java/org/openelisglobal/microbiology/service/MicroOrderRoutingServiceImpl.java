package org.openelisglobal.microbiology.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroOrderRoutingServiceImpl implements MicroOrderRoutingService {
    private final MicroCaseService caseService;
    private final MicroCaseAnalysisService caseAnalysisService;
    private final TestService testService;

    public MicroOrderRoutingServiceImpl(MicroCaseService caseService, MicroCaseAnalysisService caseAnalysisService,
            TestService testService) {
        this.caseService = caseService;
        this.caseAnalysisService = caseAnalysisService;
        this.testService = testService;
    }

    @Override
    @Transactional
    public List<MicroCase> routeAnalysesForSampleItem(SampleItem sampleItem, List<Analysis> analyses,
            String performedBy) {
        if (sampleItem == null || sampleItem.getId() == null || analyses == null || analyses.isEmpty()) {
            return List.of();
        }
        Map<String, MicroCase> cases = new LinkedHashMap<>();
        Map<String, Test> catalog = new LinkedHashMap<>();
        for (Analysis analysis : analyses) {
            if (analysis != null && analysis.getTest() != null && analysis.getTest().getId() != null) {
                catalog.computeIfAbsent(analysis.getTest().getId(), testService::get);
            }
        }
        // A set culture establishes membership before other tests on its bottle.
        List<Analysis> ordered = analyses.stream().filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        analysis -> analysis.getTest() == null || catalog.get(analysis.getTest().getId()) == null
                                || !catalog.get(analysis.getTest().getId()).isCollectedInSets()))
                .toList();
        for (Analysis analysis : ordered) {
            if (analysis.getId() == null) {
                throw new IllegalArgumentException("Order routing requires persisted analyses");
            }
            if (analysis.getSampleItem() == null || !sampleItem.getId().equals(analysis.getSampleItem().getId())) {
                throw new IllegalArgumentException("An analysis must belong to the routed specimen");
            }
            MicroCaseAnalysis existing = caseAnalysisService.getAnalysisLink(analysis.getId());
            if (existing != null) {
                MicroCase owner = caseService.getCase(existing.getCaseId());
                if (owner == null || sampleItem.getSample() == null
                        || !Objects.equals(owner.getSampleId(), sampleItem.getSample().getId())) {
                    throw new IllegalStateException("An existing analysis must belong to its original order");
                }
                cases.put(owner.getId(), owner);
                continue;
            }
            Test test = analysis.getTest() == null ? null : catalog.get(analysis.getTest().getId());
            if (test == null || !test.isOpensMicrobiologyCase()) {
                continue;
            }
            MicroCase owner = caseService.createOrGetCase(sampleItem, test, performedBy);
            caseAnalysisService.linkAnalysis(owner, analysis, performedBy);
            cases.put(owner.getId(), owner);
        }
        return List.copyOf(cases.values());
    }

    @Override
    public boolean isMicrobiologyOrder(List<Test> tests) {
        return tests != null && tests.stream().anyMatch(test -> test != null && test.isOpensMicrobiologyCase());
    }
}
