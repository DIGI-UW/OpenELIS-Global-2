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
import java.util.List;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerimport.util.MappedTestName;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;

public class CobasTaqmanDBSReader extends AnalyzerLineInserter {

    protected DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);
    protected TestService testService = SpringContext.getBean(TestService.class);
    protected TestResultService testResultService = SpringContext.getBean(TestResultService.class);

    private int ORDER_NUMBER = 0;
    private int ORDER_DATE = 0;
    private int RESULT = 0;
    private int SAMPLE_TYPE = 0;

    private final String TEST_NAME = "HIQCAP48";
    private final String DELIMITER = "\\t";
    private final String DATE_PATTERN = "yyyy/MM/dd HH:mm:ss";
    private final String VALID_PREFIXES = "LART,LDBS,LRTN,LIND,LSPE";
    private String NEGATIVE_ID;
    private String POSITIVE_ID;

    private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();
    private String error;

    public CobasTaqmanDBSReader() {
        Test test = testService.getActiveTestsByName("DNA PCR").get(0);
        List<TestResult> testResults = testResultService.getActiveTestResultsByTest(test.getId());

        for (TestResult testResult : testResults) {
            Dictionary dictionary = dictionaryService.getDataForId(testResult.getValue());
            if ("Positive".equals(dictionary.getDictEntry())) {
                POSITIVE_ID = dictionary.getId();
            } else if ("Negative".equals(dictionary.getDictEntry())) {
                NEGATIVE_ID = dictionary.getId();
            }
        }
    }

    @Override
    public boolean insert(List<String> lines, String currentUserId) {
        error = null;
        boolean successful = true;

        List<AnalyzerResults> results = new ArrayList<>();

        boolean columnsFound = manageColumns(lines.get(0));

        if (!columnsFound) {
            error = "Cobas Taqman DBS analyzer: Unable to find correct columns in file #";
            return false;
        }

        MappedTestName mappedName = AnalyzerTestNameCache.getInstance().getMappedTest(AnalyzerTestNameCache.COBAS_DBS,
                TEST_NAME);

        if (mappedName == null) {
            mappedName = AnalyzerTestNameCache.getInstance().getEmptyMappedTestName(AnalyzerTestNameCache.COBAS_DBS,
                    TEST_NAME);
        }

        for (int i = 1; i < lines.size(); ++i) {
            createAnalyzerResultFromLine(lines.get(i), results, mappedName);
        }

        if (results.size() > 0) {
            try {
                persistResults(results, currentUserId);
            } catch (LIMSRuntimeException e) {
                error = "Cobas Taqman DBS analyzer: Unable to save to database";
                successful = false;
            }
        }
        return successful;
    }

    private boolean manageColumns(String line) {
        String[] fields = line.split(DELIMITER);

        for (int i = 0; i < fields.length; i++) {
            String header = fields[i].replace("\"", "");

            if ("Order Number".equals(header)) {
                ORDER_NUMBER = i;
            } else if ("Order Date/Time".equals(header)) {
                ORDER_DATE = i;
            } else if ("Result".equals(header)) {
                RESULT = i;
            } else if ("Sample Type".equals(header)) {
                SAMPLE_TYPE = i;
            }
        }
        return ORDER_DATE != 0 && ORDER_NUMBER != 0 && RESULT != 0 && SAMPLE_TYPE != 0;
    }

    private void addValueToResults(List<AnalyzerResults> resultList, AnalyzerResults result) {
        resultList.add(result);

        AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(result);
        if (resultFromDB != null) {
            resultList.add(resultFromDB);
        }
    }

    private void createAnalyzerResultFromLine(String line, List<AnalyzerResults> resultList,
            MappedTestName mappedName) {
        String[] fields = line.split(DELIMITER);

        AnalyzerResults analyzerResults = new AnalyzerResults();

        String result = getAppropriateResults(fields[RESULT]);
        String accessionNumber = fields[ORDER_NUMBER].replace("\"", "").trim();

        analyzerResults.setAnalyzerId(mappedName.getAnalyzerId());
        analyzerResults.setResult(result);
        analyzerResults.setCompleteDate(DateUtil
                .convertStringDateToTimestampWithPattern(fields[ORDER_DATE].replace("\"", "").trim(), DATE_PATTERN));
        analyzerResults.setTestId(mappedName.getTestId());
        analyzerResults.setIsControl(!VALID_PREFIXES.contains(accessionNumber.subSequence(0, 3)));
        analyzerResults.setTestName(mappedName.getOpenElisTestName());
        analyzerResults.setResultType("D");

        if (analyzerResults.getIsControl()) {
            if (!"S".equals(fields[SAMPLE_TYPE].replace("\"", "").trim())) {
                accessionNumber += ":" + fields[SAMPLE_TYPE].replace("\"", "").trim();
            }
        } else {
            accessionNumber = accessionNumber.substring(0, 9);
        }

        analyzerResults.setAccessionNumber(accessionNumber);

        addValueToResults(resultList, analyzerResults);
    }

    private String getAppropriateResults(String result) {
        result = result.replace("\"", "").trim();
        if ("Target Not Detected".equals(result)) {
            result = NEGATIVE_ID;
        } else {
            result = POSITIVE_ID;
            // save this until we finish w/ requirements
            // String workingResult = result.split("\\(")[0].replace("<", "").replace("E",
            // "");
            // String[] splitResult = workingResult.split("\\+");
            //
            // if (Double.parseDouble(splitResult[0]) * Math.pow(10,
            // Double.parseDouble(splitResult[1]))
            // < THREASHOLD) {
            // result = UNDER_THREASHOLD;
            // }
        }
        return result;
    }

    @Override
    public String getError() {
        return error;
    }
}
