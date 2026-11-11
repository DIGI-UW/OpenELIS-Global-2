package org.openelisglobal.orderentry.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.orderentry.dao.OrderTestTestedElsewhereDAO;
import org.openelisglobal.orderentry.valueholder.OrderTestTestedElsewhere;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampletyperequest.service.SampleTypeRequestService;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestedElsewhereServiceImpl implements TestedElsewhereService {

    static final int MAX_VALUE_LENGTH = 255;

    @Autowired
    private OrderTestTestedElsewhereDAO testedElsewhereDAO;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleTypeRequestService sampleTypeRequestService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private PanelItemService panelItemService;

    @Autowired
    private OrganizationService organizationService;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> forOrder(String labNumber) {
        Sample sample = requireOrder(labNumber);
        List<Map<String, Object>> marks = new ArrayList<>();
        for (OrderTestTestedElsewhere mark : testedElsewhereDAO.forSample(Integer.valueOf(sample.getId()))) {
            marks.add(view(mark));
        }
        return marks;
    }

    @Override
    @Transactional
    public Map<String, Object> mark(String labNumber, String testId, String performingLabId, String reportedValue,
            String sysUserId) {
        Sample sample = requireOrder(labNumber);
        if (!GenericValidator.isInt(testId) || !testsOnOrder(sample).contains(testId.trim())) {
            throw new OrderEntryRequestRefusedException("the test is not on this order");
        }
        Integer labId = null;
        if (!GenericValidator.isBlankOrNull(performingLabId)) {
            if (!GenericValidator.isInt(performingLabId.trim())
                    || organizationService.getOrganizationById(performingLabId.trim()) == null) {
                throw new OrderEntryRequestRefusedException("unknown performing laboratory");
            }
            labId = Integer.valueOf(performingLabId.trim());
        }
        String value = reportedValue == null ? null : reportedValue.trim();
        if (value != null && value.length() > MAX_VALUE_LENGTH) {
            throw new OrderEntryRequestRefusedException("the reported value is longer than " + MAX_VALUE_LENGTH);
        }
        Integer sampleId = Integer.valueOf(sample.getId());
        Integer test = Integer.valueOf(testId.trim());
        Optional<OrderTestTestedElsewhere> existing = testedElsewhereDAO.forSampleAndTest(sampleId, test);
        OrderTestTestedElsewhere mark = existing.orElseGet(OrderTestTestedElsewhere::new);
        mark.setSampleId(sampleId);
        mark.setTestId(test);
        mark.setPerformingLabId(labId);
        mark.setReportedValue(value == null || value.isEmpty() ? null : value);
        mark.setRecordedById(GenericValidator.isInt(sysUserId) ? Integer.valueOf(sysUserId) : null);
        mark.setSysUserId(sysUserId);
        if (existing.isPresent()) {
            mark = testedElsewhereDAO.update(mark);
        } else {
            mark.setId(testedElsewhereDAO.insert(mark));
        }
        return view(mark);
    }

    @Override
    @Transactional
    public boolean unmark(String labNumber, String testId) {
        Sample sample = requireOrder(labNumber);
        if (!GenericValidator.isInt(testId)) {
            return false;
        }
        Optional<OrderTestTestedElsewhere> existing = testedElsewhereDAO
                .forSampleAndTest(Integer.valueOf(sample.getId()), Integer.valueOf(testId.trim()));
        existing.ifPresent(testedElsewhereDAO::delete);
        return existing.isPresent();
    }

    /**
     * The tests the order carries: those requested at Enter Order (directly or as
     * members of a requested panel) and those that already have an analysis.
     */
    Set<String> testsOnOrder(Sample sample) {
        Set<String> testIds = new HashSet<>();
        for (SampleTypeRequest request : sampleTypeRequestService.getRequestsBySampleId(sample.getId())) {
            addCsv(testIds, request.getRequestedTests());
            if (!GenericValidator.isBlankOrNull(request.getRequestedPanels())) {
                for (String panelId : request.getRequestedPanels().split(",")) {
                    if (GenericValidator.isBlankOrNull(panelId)) {
                        continue;
                    }
                    for (PanelItem item : panelItemService.getPanelItemsForPanel(panelId.trim())) {
                        if (item.getTest() != null) {
                            testIds.add(item.getTest().getId());
                        }
                    }
                }
            }
        }
        for (Analysis analysis : analysisService.getAnalysesBySampleId(sample.getId())) {
            if (analysis.getTest() != null) {
                testIds.add(analysis.getTest().getId());
            }
        }
        return testIds;
    }

    private Sample requireOrder(String labNumber) {
        Sample sample = GenericValidator.isBlankOrNull(labNumber) ? null
                : sampleService.getSampleByAccessionNumber(labNumber.trim());
        if (sample == null || sample.getId() == null) {
            throw new OrderEntryRequestRefusedException("no order has lab number " + labNumber);
        }
        return sample;
    }

    private Map<String, Object> view(OrderTestTestedElsewhere mark) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("testId", String.valueOf(mark.getTestId()));
        String labId = mark.getPerformingLabId() == null ? "" : String.valueOf(mark.getPerformingLabId());
        view.put("performingLabId", labId);
        Organization lab = labId.isEmpty() ? null : organizationService.getOrganizationById(labId);
        view.put("performingLabName", lab == null ? "" : lab.getOrganizationName());
        view.put("reportedValue", mark.getReportedValue() == null ? "" : mark.getReportedValue());
        return view;
    }

    private static void addCsv(Set<String> into, String csv) {
        if (GenericValidator.isBlankOrNull(csv)) {
            return;
        }
        for (String id : csv.split(",")) {
            if (!GenericValidator.isBlankOrNull(id)) {
                into.add(id.trim());
            }
        }
    }
}
