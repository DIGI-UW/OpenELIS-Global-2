package org.openelisglobal.result.controller.rest;

import java.lang.reflect.InvocationTargetException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.inventory.action.InventoryUtility;
import org.openelisglobal.inventory.form.InventoryKitItem;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.openelisglobal.result.form.StatusResultsForm;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.beanItems.TestResultItem;

public class LogbookStatusResults {

    private final AnalysisService analysisService;

    private final SampleService sampleService;

    private final SampleItemService sampleItemService;

    private final InventoryUtility inventoryUtility = SpringContext.getBean(InventoryUtility.class);

    private static final boolean REVERSE_SORT_ORDER = false;

    private static final Set<Integer> excludedStatusIds;

    static {
        // currently this is the only one being excluded for Haiti_LNSP. If it
        // gets more complicate use the status sets
        excludedStatusIds = new HashSet<>();
        excludedStatusIds.add(Integer.parseInt(
                SpringContext.getBean(IStatusService.class).getStatusID(StatusService.AnalysisStatus.Canceled)));
        excludedStatusIds.add(Integer.parseInt(
                SpringContext.getBean(IStatusService.class).getStatusID(StatusService.AnalysisStatus.SampleRejected)));
    }

    public LogbookStatusResults(AnalysisService analysisService, SampleService sampleService,
            SampleItemService sampleItemService) {
        this.analysisService = analysisService;
        this.sampleService = sampleService;
        this.sampleItemService = sampleItemService;
    }

    public List<TestResultItem> setSearchResults(StatusResultsForm form, ResultsLoadUtility resultsUtility)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        List<TestResultItem> tests = getSelectedTests(form, resultsUtility);
        form.setSearchFinished(Boolean.TRUE);

        if (resultsUtility.inventoryNeeded()) {
            addInventory(form);
            form.setDisplayTestKit(true);
        } else {
            addEmptyInventoryList(form);
            form.setDisplayTestKit(false);
        }

        return tests;
    }

    private List<TestResultItem> getSelectedTests(StatusResultsForm form, ResultsLoadUtility resultsUtility) {
        String collectionDate = form.getCollectionDate();
        String receivedDate = form.getRecievedDate();
        String analysisStatus = form.getSelectedAnalysisStatus();
        String sampleStatus = form.getSelectedSampleStatus();
        String test = form.getSelectedTest();

        List<Analysis> analysisList = new ArrayList<>();

        if (!GenericValidator.isBlankOrNull(collectionDate)) {
            analysisList = getAnalysisForCollectionDate(collectionDate);
            if (analysisList.isEmpty()) {
                return new ArrayList<>();
            }
        }

        if (!GenericValidator.isBlankOrNull(receivedDate)) {
            analysisList = blendLists(analysisList, getAnalysisForRecievedDate(receivedDate));
            if (analysisList.isEmpty()) {
                return new ArrayList<>();
            }
        }

        if (!(GenericValidator.isBlankOrNull(analysisStatus) || analysisStatus.equals("0"))) {
            analysisList = blendLists(analysisList, getAnalysisForAnalysisStatus(analysisStatus));
            if (analysisList.isEmpty()) {
                return new ArrayList<>();
            }
        }

        if (!(GenericValidator.isBlankOrNull(sampleStatus) || sampleStatus.equals("0"))) {
            analysisList = blendLists(analysisList, getAnalysisForSampleStatus(sampleStatus));
            if (analysisList.isEmpty()) {
                return new ArrayList<>();
            }
        }

        if (!(GenericValidator.isBlankOrNull(test) || test.equals("0"))) {
            analysisList = blendLists(analysisList, getAnalysisForTest(test));
            if (analysisList.isEmpty()) {
                return new ArrayList<>();
            }
        }

        return buildTestItems(analysisList, resultsUtility);
    }

    private List<Analysis> getAnalysisForCollectionDate(String collectionDate) {
        Date date = DateUtil.convertStringDateToSqlDate(collectionDate);
        return analysisService.getAnalysisCollectedOnExcludedByStatusId(date, excludedStatusIds);
    }

    private List<Analysis> blendLists(List<Analysis> masterList, List<Analysis> newList) {
        if (masterList.isEmpty()) {
            return newList;
        } else {
            List<Analysis> blendedList = new ArrayList<>();
            for (Analysis master : masterList) {
                for (Analysis newAnalysis : newList) {
                    if (master.getId().equals(newAnalysis.getId())) {
                        blendedList.add(master);
                    }
                }
            }
            return blendedList;
        }
    }

    private List<TestResultItem> buildTestItems(List<Analysis> analysisList, ResultsLoadUtility resultsUtility) {
        if (analysisList.isEmpty()) {
            return new ArrayList<>();
        }

        return resultsUtility.getGroupedTestsForAnalysisList(analysisList, REVERSE_SORT_ORDER);
    }

    private List<Analysis> getAnalysisForRecievedDate(String recievedDate) {
        List<Sample> sampleList = sampleService.getSamplesReceivedOn(recievedDate);

        return getAnalysisListForSampleItems(sampleList);
    }

    private List<Analysis> getAnalysisListForSampleItems(List<Sample> sampleList) {
        List<Analysis> analysisList = new ArrayList<>();

        for (Sample sample : sampleList) {
            List<SampleItem> sampleItemList = sampleItemService.getSampleItemsBySampleId(sample.getId());

            for (SampleItem sampleItem : sampleItemList) {
                List<Analysis> analysisListForItem = analysisService
                        .getAnalysesBySampleItemsExcludingByStatusIds(sampleItem, excludedStatusIds);

                analysisList.addAll(analysisListForItem);
            }
        }

        return analysisList;
    }

    private List<Analysis> getAnalysisForAnalysisStatus(String status) {
        return analysisService.getAnalysesForStatusId(status);
    }

    private List<Analysis> getAnalysisForSampleStatus(String sampleStatus) {
        return analysisService.getAnalysesBySampleStatusIdExcludingByStatusId(sampleStatus, excludedStatusIds);
    }

    private List<Analysis> getAnalysisForTest(String testId) {
        List<Integer> excludedStatusIntList = new ArrayList<>();
        excludedStatusIntList.addAll(excludedStatusIds);
        return analysisService.getAllAnalysisByTestAndExcludedStatus(testId, excludedStatusIntList);
    }

    private void addInventory(StatusResultsForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        List<InventoryKitItem> list = inventoryUtility.getExistingActiveInventory();
        form.setInventoryItems(list);
    }

    private void addEmptyInventoryList(StatusResultsForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        form.setInventoryItems(new ArrayList<InventoryKitItem>());
    }
}
