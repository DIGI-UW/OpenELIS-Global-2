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
package org.openelisglobal.dataexchange.aggregatereporting;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportExternalExport;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportQueueType;
import org.openelisglobal.dataexchange.service.aggregatereporting.ReportExternalExportService;
import org.openelisglobal.dataexchange.service.aggregatereporting.ReportQueueTypeService;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.spring.util.SpringContext;

public class TestUsageUpdate implements IResultUpdate {

    private static String TEST_USAGE_TYPE_ID;
    private ReportExternalExportService queueService = SpringContext.getBean(ReportExternalExportService.class);
    private ReportQueueTypeService reportQueueTypeService = SpringContext.getBean(ReportQueueTypeService.class);
    private static ContainerFactory CONTAINER_FACTORY;

    public TestUsageUpdate() {
        ReportQueueType queueType = reportQueueTypeService.getReportQueueTypeByName("labIndicator");
        if (queueType != null) {
            TEST_USAGE_TYPE_ID = queueType.getId();
        }

        CONTAINER_FACTORY = new ContainerFactory() {
            @Override
            @SuppressWarnings("rawtypes")
            public List creatArrayContainer() {
                return new ArrayList();
            }

            @Override
            public Map<String, Long> createObjectContainer() {
                return new HashMap<>();
            }
        };
    }

    @Override
    public void transactionalUpdate(IResultSaveService resultService) throws LIMSRuntimeException {
        // TODO Auto-generated method stub

    }

    @Override
    public void postTransactionalCommitUpdate(IResultSaveService resultSaveService) {
        Map<String, Map<String, Integer>> dateTestMap = new HashMap<>();
        List<ReportExternalExport> exports = new ArrayList<>();
        List<Result> results = getAllResults(resultSaveService);

        createMaps(dateTestMap, results);

        updateData(dateTestMap, exports);

        applyUpdatesToDB(exports);
    }

    private List<Result> getAllResults(IResultSaveService resultSaveService) {
        List<Result> results = new ArrayList<>();

        for (ResultSet resultSet : resultSaveService.getNewResults()) {
            results.add(resultSet.result);
        }

        for (ResultSet resultSet : resultSaveService.getModifiedResults()) {
            results.add(resultSet.result);
        }
        return results;
    }

    private void createMaps(Map<String, Map<String, Integer>> dateTestMap, List<Result> results) {
        for (Result result : results) {
            ResultService resultResultService = SpringContext.getBean(ResultService.class);
            String testDate = resultResultService.getTestTime(result);
            if (testDate == null) {
                testDate = resultResultService.getLastUpdatedTime(result);
            }
            Map<String, Integer> testCountMap = dateTestMap.get(testDate);

            if (testCountMap == null) {
                testCountMap = new HashMap<>();
                dateTestMap.put(testDate, testCountMap);
            }

            String testDescription = resultResultService.getTestDescription(result);

            Integer count = testCountMap.get(testDescription);
            testCountMap.put(testDescription, count == null ? 1 : count + 1);
        }
    }

    private void updateData(Map<String, Map<String, Integer>> dateTestMap, List<ReportExternalExport> exports) {
        for (String date : dateTestMap.keySet()) {
            ReportExternalExport export = new ReportExternalExport();
            export.setTypeId(TEST_USAGE_TYPE_ID);
            export.setEventDate(DateUtil.convertStringDateToTruncatedTimestamp(date));
            export = queueService.getReportByEventDateAndType(export);

            updateExport(export, dateTestMap.get(date));

            export.setCollectionDate(DateUtil.getNowAsTimestamp());

            exports.add(export);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateExport(ReportExternalExport export, Map<String, Integer> testCountMap) {
        JSONParser parser = new JSONParser();
        Map<String, Long> databaseTestCountList = null;

        if (export.getData() == null) {
            export.setData("{}");
        }

        export.setSend(true);

        JSONObject json = new JSONObject();
        try {
            databaseTestCountList = (Map<String, Long>) parser.parse(export.getData().replace("\n", ""),
                    CONTAINER_FACTORY);

            for (String test : testCountMap.keySet()) {
                Long count = databaseTestCountList.get(test);
                databaseTestCountList.put(test, count == null ? 1 : count + testCountMap.get(test));
            }

            for (String name : databaseTestCountList.keySet()) {
                json.put(name, databaseTestCountList.get(name));
            }
        } catch (ParseException e) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "updateExport", e.toString());
        }

        StringWriter buffer = new StringWriter();
        try {
            json.writeJSONString(buffer);
        } catch (IOException e) {
            LogEvent.logDebug(e);
        }

        String data = buffer.toString().replace("\n", "");
        export.setData(data);
    }

    private void applyUpdatesToDB(List<ReportExternalExport> exports) {
        try {
            queueService.saveAll(exports);
            // for (ReportExternalExport export : exports) {
            // if (export.getId() == null) {
            // queueService.insertReportExternalExport(export);
            // } else {
            // queueService.updateReportExternalExport(export);
            // }
            // }
        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e);
        }
    }
}
