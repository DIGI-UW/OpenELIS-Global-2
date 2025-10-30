/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.common.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("prototype")
@DependsOn({ "springContext" })
public class SampleAddService {
    private final String xml;
    private final String currentUserId;
    private final Sample sample;
    private final List<SampleTestCollection> sampleItemsTests = new ArrayList<>();
    private final String receivedDate;
    private final Map<String, Panel> panelIdPanelMap = new HashMap<>();
    private boolean xmlProcessed = false;
    private int sampleItemIdIndex = 0;
    private static final boolean USE_RECEIVE_DATE_FOR_COLLECTION_DATE = !FormFields.getInstance()
            .useField(Field.CollectionDate);

    private static TypeOfSampleService typeOfSampleService = SpringContext.getBean(TypeOfSampleService.class);
    private static PanelService panelService = SpringContext.getBean(PanelService.class);
    private static PanelItemService panelItemService = SpringContext.getBean(PanelItemService.class);
    private static ObservationHistoryTypeService ohtService = SpringContext
            .getBean(ObservationHistoryTypeService.class);
    private static UnitOfMeasureService unitOfMeasureService = SpringContext.getBean(UnitOfMeasureService.class);

    private static String getObservationHistoryTypeId(String name) {
        ObservationHistoryType oht;
        oht = ohtService.getByName(name);
        if (oht != null) {
            return oht.getId();
        }

        return null;
    }

    public SampleAddService(String xml, String currentUserId, Sample sample, String receiveDate) {
        this.xml = xml;
        this.currentUserId = currentUserId;
        this.sample = sample;
        receivedDate = receiveDate;
    }

    public List<SampleTestCollection> createSampleTestCollection() {
        xmlProcessed = true;
        String collectionDateFromRecieveDate = null;
        if (USE_RECEIVE_DATE_FOR_COLLECTION_DATE) {
            collectionDateFromRecieveDate = receivedDate + " 00:00:00";
        }

        try {
            Document sampleDom = DocumentHelper.parseText(xml);

            for (@SuppressWarnings("rawtypes")
            Iterator i = sampleDom.getRootElement().elementIterator("sample"); i.hasNext();) {
                sampleItemIdIndex++;

                Element sampleItem = (Element) i.next();

                String testIDs = sampleItem.attributeValue("tests");
                String panelIDs = sampleItem.attributeValue("panels");
                Map<String, String> testIdToUserSectionMap = getTestIdToSelectionMap(
                        sampleItem.attributeValue("testSectionMap"));
                Map<String, String> testIdToSampleTypeMap = getTestIdToSelectionMap(
                        sampleItem.attributeValue("testSampleTypeMap"));

                String collectionDate = sampleItem.attributeValue("date") == null ? null
                        : sampleItem.attributeValue("date").trim();
                String collectionTime = sampleItem.attributeValue("time") == null ? null
                        : sampleItem.attributeValue("time").trim();
                String collectionDateTime = null;
                String rejectedValue = sampleItem.attributeValue("rejected") == null ? null
                        : sampleItem.attributeValue("rejected").trim();
                boolean rejected = StringUtils.isNotBlank(rejectedValue) ? Boolean.parseBoolean(rejectedValue) : false;
                String rejectReasonId = sampleItem.attributeValue("rejectReasonId") == null ? null
                        : sampleItem.attributeValue("rejectReasonId").trim();

                if (!GenericValidator.isBlankOrNull(collectionDate)
                        && !GenericValidator.isBlankOrNull(collectionTime)) {
                    collectionDateTime = collectionDate + " " + collectionTime;
                } else if (!GenericValidator.isBlankOrNull(collectionDate)
                        && GenericValidator.isBlankOrNull(collectionTime)) {
                    collectionDateTime = collectionDate + " 00:00";
                }

                augmentPanelIdToPanelMap(panelIDs);
                List<ObservationHistory> initialConditionList = null;
                if (FormFields.getInstance().useField(Field.InitialSampleCondition)) {
                    initialConditionList = addInitialSampleConditions(sampleItem, initialConditionList);
                }
                ObservationHistory sampleNature = null;
                if (FormFields.getInstance().useField(Field.SampleNature)) {
                    sampleNature = getSampleNature(sampleItem);
                }

                SampleItem item = new SampleItem();
                item.setSysUserId(currentUserId);
                item.setSample(sample);
                item.setTypeOfSample(typeOfSampleService.getTypeOfSampleById(sampleItem.attributeValue("sampleID")));
                item.setSortOrder(Integer.toString(sampleItemIdIndex));
                if (rejected) {
                    item.setStatusId(
                            SpringContext.getBean(IStatusService.class).getStatusID(SampleStatus.SampleRejected));
                } else {
                    item.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(SampleStatus.Entered));
                }
                item.setCollector(sampleItem.attributeValue("collector"));

                String quantityStr = sampleItem.attributeValue("quantity");
                if (quantityStr != null && !quantityStr.trim().isEmpty()) {
                    item.setQuantity(Double.valueOf(quantityStr));
                }

                item.setExternalId(sample.getAccessionNumber() + "-" + sampleItemIdIndex);

                String uomId = sampleItem.attributeValue("uom");
                if (uomId != null && !uomId.trim().isEmpty()) {
                    item.setUnitOfMeasure(unitOfMeasureService.getUnitOfMeasureById(uomId));
                }

                item.setRejected(rejected);
                item.setRejectReasonId(rejectReasonId);

                if (!GenericValidator.isBlankOrNull(collectionDateTime)) {
                    item.setCollectionDate(DateUtil.convertStringDateToTimestamp(collectionDateTime));
                }
                List<Test> tests = new ArrayList<>();

                addTests(testIDs, tests);

                sampleItemsTests.add(new SampleTestCollection(item, tests,
                        USE_RECEIVE_DATE_FOR_COLLECTION_DATE ? collectionDateFromRecieveDate : collectionDateTime,
                        initialConditionList, testIdToUserSectionMap, testIdToSampleTypeMap, sampleNature));
            }
        } catch (DocumentException e) {
            LogEvent.logDebug(e);
        }

        return sampleItemsTests;
    }

    public Panel getPanelForTest(Test test) throws IllegalThreadStateException {
        if (!xmlProcessed) {
            throw new IllegalThreadStateException("createSampleTestCollection must be called first");
        }

        List<PanelItem> panelItems = panelItemService.getPanelItemByTestId(test.getId());

        for (PanelItem panelItem : panelItems) {
            Panel panel = panelIdPanelMap.get(panelItem.getPanel().getId());
            if (panel != null) {
                return panel;
            }
        }

        return null;
    }

    public void setInitialSampleItemOrderValue(int initialValue) {
        sampleItemIdIndex = initialValue;
    }

    private Map<String, String> getTestIdToSelectionMap(String mapPairs) {
        Map<String, String> sectionMap = new HashMap<>();

        String[] maps = mapPairs.split(",");
        for (String map : maps) {
            String[] mapping = map.split(":");
            if (mapping.length == 2) {
                sectionMap.put(mapping[0].trim(), mapping[1].trim());
            }
        }

        return sectionMap;
    }

    private void augmentPanelIdToPanelMap(String panelIDs) {
        if (panelIDs != null) {
            String[] ids = panelIDs.split(",");
            for (String id : ids) {
                if (!GenericValidator.isBlankOrNull(id)) {
                    panelIdPanelMap.put(id, panelService.getPanelById(id));
                }
            }
        }
    }

    private List<ObservationHistory> addInitialSampleConditions(Element sampleItem,
            List<ObservationHistory> initialConditionList) {
        String initialSampleConditionIdString = sampleItem.attributeValue("initialConditionIds");
        if (!GenericValidator.isBlankOrNull(initialSampleConditionIdString)) {
            String[] initialSampleConditionIds = initialSampleConditionIdString.split(",");
            initialConditionList = new ArrayList<>();

            for (int j = 0; j < initialSampleConditionIds.length; ++j) {
                ObservationHistory initialSampleConditions = new ObservationHistory();
                initialSampleConditions.setValue(initialSampleConditionIds[j]);
                initialSampleConditions.setValueType(ObservationHistory.ValueType.DICTIONARY);
                initialSampleConditions
                        .setObservationHistoryTypeId(getObservationHistoryTypeId("initialSampleCondition"));
                initialConditionList.add(initialSampleConditions);
            }
        }
        return initialConditionList;
    }

    private ObservationHistory getSampleNature(Element sampleItem) {
        String sampleNatureId = sampleItem.attributeValue("sampleNatureId");
        ObservationHistory sampleNature = new ObservationHistory();
        if (!GenericValidator.isBlankOrNull(sampleNatureId)) {

            sampleNature.setValue(sampleNatureId);
            sampleNature.setValueType(ObservationHistory.ValueType.DICTIONARY);
            sampleNature.setObservationHistoryTypeId(getObservationHistoryTypeId("sampleNature"));
        }
        return sampleNature;
    }

    private void addTests(String testIDs, List<Test> tests) {
        StringTokenizer tokenizer = new StringTokenizer(testIDs, ",");

        while (tokenizer.hasMoreTokens()) {
            Test test = new Test();
            test.setId(tokenizer.nextToken().trim());
            tests.add(test);
        }
    }

    public final class SampleTestCollection {
        public SampleItem item;

        public List<Test> tests;
        public String collectionDate;
        public List<ObservationHistory> initialSampleConditionIdList;
        public Map<String, String> testIdToUserSectionMap;
        public Map<String, String> testIdToUserSampleTypeMap;
        public ObservationHistory sampleNature;

        // gets added as they are persisted
        public List<Analysis> analysises;

        public SampleTestCollection(SampleItem item, List<Test> tests, String collectionDate,
                List<ObservationHistory> initialConditionList, Map<String, String> testIdToUserSectionMap,
                Map<String, String> testIdToUserSampleTypeMap, ObservationHistory sampleNature) {
            this.item = item;
            this.tests = tests;
            this.collectionDate = collectionDate;
            this.testIdToUserSectionMap = testIdToUserSectionMap;
            this.testIdToUserSampleTypeMap = testIdToUserSampleTypeMap;
            initialSampleConditionIdList = initialConditionList;
            this.sampleNature = sampleNature;
        }
    }
}
