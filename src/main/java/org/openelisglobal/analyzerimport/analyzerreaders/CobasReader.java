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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.analyzerimport.analyzerreaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerimport.util.MappedTestName;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;

public class CobasReader extends AnalyzerLineInserter {

    protected TestService testService = SpringContext.getBean(TestService.class);

    private final String COBAS_INTEGRA400_NAME = "Cobas Integra";
    private String ASTL_ID;
    private String ALTL_ID;
    private String CRE_ID;
    private String GLU_ID;
    private Map<String, Integer> testIdToPresentation;

    // private final int ID = 0;
    private final int DATE = 1;
    private final int TEST = 2;
    // private final int BLANK1 = 3;
    private final int ACCESSION = 4;
    // private final int BLANK2 = 5;
    // private final int SAMPLE_TYPE = 6;
    private final int UNITS = 7;
    // private final int BLANK3 = 8;
    private final int MAJOR_RESULTS = 9;
    // private final int MINOR_RESULTS = 10;
    // private final int INDICATOR_1 = 11;
    // private final int INDICATOR_2 = 12;

    private final String DELIMITER = "\\t";
    private final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();

    public CobasReader() {
        // TestDAO testDAO = new TestDAOImpl();
        ASTL_ID = testService.getActiveTestsByName("Transaminases ASTL").get(0).getId();
        ALTL_ID = testService.getActiveTestsByName("Transaminases ALTL").get(0).getId();
        CRE_ID = testService.getActiveTestsByName("Créatininémie").get(0).getId();
        GLU_ID = testService.getActiveTestsByName("Glycémie").get(0).getId();

        testIdToPresentation = new HashMap<>();
        testIdToPresentation.put(ALTL_ID, 0);
        testIdToPresentation.put(ASTL_ID, 1);
        testIdToPresentation.put(CRE_ID, 2);
        testIdToPresentation.put(GLU_ID, 3);
    }

    @Override
    public boolean insert(List<String> lines, String currentUserId) {
        boolean successful = true;

        if (lines == null) {
            return true;
        }

        if (GenericValidator.isBlankOrNull(currentUserId)) {
            return false;
        }

        List<AnalyzerResults> results = new ArrayList<>();
        List<AnalyzerResults> notMatchedResults = new ArrayList<>();

        Map<String, AnalyzerResults[]> accessionToResultMap = new HashMap<>();
        List<String> accessionOrder = new ArrayList<>();

        for (String line : lines) {
            createAnalyzerResultFromLine(line, accessionToResultMap, accessionOrder, notMatchedResults);
        }

        for (String accessionNumber : accessionOrder) {

            AnalyzerResults[] resultSet = accessionToResultMap.get(accessionNumber);

            for (int i = 0; i < 4; i++) {
                if (resultSet[i] != null) {
                    addValueToResults(results, resultSet[i]);
                }
            }

            for (AnalyzerResults analyzerResult : notMatchedResults) {
                addValueToResults(results, analyzerResult);
            }
        }

        if (results.size() > 0) {

            // ensure transaction block
            try {
                persistResults(results, currentUserId);
            } catch (LIMSRuntimeException e) {
                successful = false;
            }
        }

        return successful;
    }

    private void addValueToResults(List<AnalyzerResults> resultList, AnalyzerResults result) {
        resultList.add(result);

        AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(result);
        if (resultFromDB != null) {
            resultList.add(resultFromDB);
        }
    }

    private void createAnalyzerResultFromLine(String line, Map<String, AnalyzerResults[]> accessionToResultMap,
            List<String> accessionOrder, List<AnalyzerResults> notMatchedResults) {
        String[] fields = line.split(DELIMITER);

        AnalyzerResults analyzerResults = new AnalyzerResults();
        MappedTestName mappedName = AnalyzerTestNameCache.getInstance().getMappedTest(COBAS_INTEGRA400_NAME,
                fields[TEST]);

        if (mappedName == null) {
            mappedName = AnalyzerTestNameCache.getInstance().getEmptyMappedTestName(COBAS_INTEGRA400_NAME,
                    fields[TEST]);
        }

        analyzerResults.setAnalyzerId(mappedName.getAnalyzerId());
        analyzerResults.setResult(fields[MAJOR_RESULTS]);
        analyzerResults.setUnits(fields[UNITS]);
        analyzerResults.setCompleteDate(DateUtil.convertStringDateToTimestampWithPattern(fields[DATE], DATE_PATTERN));
        analyzerResults.setAccessionNumber(fields[ACCESSION]);
        analyzerResults.setTestId(mappedName.getTestId());

        if (analyzerResults.getAccessionNumber() != null) {
            analyzerResults.setIsControl(analyzerResults.getAccessionNumber().trim().contains(" "));
        } else {
            analyzerResults.setIsControl(false);
        }

        analyzerResults.setTestName(mappedName.getOpenElisTestName());

        if (analyzerResults.getTestId() != null) {
            int bufferIndex = testIdToPresentation.get(analyzerResults.getTestId()).intValue();
            AnalyzerResults[] buffer = accessionToResultMap.get(analyzerResults.getAccessionNumber());

            if (buffer == null) {
                buffer = new AnalyzerResults[4];
                accessionToResultMap.put(analyzerResults.getAccessionNumber(), buffer);
                accessionOrder.add(analyzerResults.getAccessionNumber());
            }
            buffer[bufferIndex] = analyzerResults;
        } else {
            notMatchedResults.add(analyzerResults);
        }
    }

    @Override
    public String getError() {
        return "Cobas Intgra 400 error writting to database";
    }
}
