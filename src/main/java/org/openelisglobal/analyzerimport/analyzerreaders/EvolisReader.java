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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerimport.util.MappedTestName;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;

public class EvolisReader extends AnalyzerLineInserter {

    protected DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);
    protected TestService testService = SpringContext.getBean(TestService.class);
    protected TestResultService testResultService = SpringContext.getBean(TestResultService.class);

    private String NEGATIVE_DICTIONARY_ID = null;
    private String POSITIVE_DICTIONARY_ID = null;
    private String INDETERMINATE_DICTIONARY_ID = null;
    private String DELIMITER = "|";
    private int Id = 0;
    private int assay = 1;
    private int well = 2;
    private int flag = 3;
    private int value = 4;
    private int S_CO = 5;
    private int result = 6;

    private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();

    public EvolisReader() {
        Test test = testService.getTestByLocalizedName("Integral", Locale.ENGLISH); // integral and murex use the same
        // dictionary values

        List<TestResult> testResults = testResultService.getActiveTestResultsByTest(test.getId());

        for (TestResult testResult : testResults) {
            String dictionaryValue = dictionaryService.getDictionaryById(testResult.getValue()).getDictEntry();

            if ("Positive".equals(dictionaryValue)) {
                POSITIVE_DICTIONARY_ID = testResult.getValue();
            } else if ("Negative".equals(dictionaryValue)) {
                NEGATIVE_DICTIONARY_ID = testResult.getValue();
            } else if ("Indeterminate".equals(dictionaryValue)) {
                INDETERMINATE_DICTIONARY_ID = testResult.getValue();
            }
        }
    }

    @Override
    public boolean insert(List<String> lines, String currentUserId) {

        boolean successful = true;

        List<AnalyzerResults> results = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            addAnalyzerResultFromLine(results, lines.get(i));
        }

        if (results.size() > 0) {

            // ensure transaction block
            try {
                persistResults(results, currentUserId);
            } catch (LIMSRuntimeException e) {
                LogEvent.logDebug(e);
                successful = false;
            }
        }
        return successful;
    }

    private void addAnalyzerResultFromLine(List<AnalyzerResults> results, String line) {
        line = line.replace("\"", "").replace(DELIMITER, ":");
        String[] fields = line.split(":");

        String analyzerAccessionNumber = fields[Id];

        if (fields.length == 7 && !GenericValidator.isBlankOrNull(analyzerAccessionNumber)
                && analyzerAccessionNumber.length() > 6 && fields[assay].length() > 5) {

            MappedTestName mappedName = AnalyzerTestNameCache.getInstance().getMappedTest(AnalyzerTestNameCache.EVOLIS,
                    fields[assay]);
            AnalyzerResults analyzerResults = new AnalyzerResults();
            analyzerResults.setAnalyzerId(mappedName.getAnalyzerId());
            analyzerResults.setResult(getDictioanryValueForResult(fields[result]));
            analyzerResults.setResultType("D");
            analyzerResults.setCompleteDate(new Timestamp(new Date().getTime()));
            analyzerResults.setTestId(mappedName.getTestId());
            analyzerResults.setAccessionNumber(analyzerAccessionNumber);
            analyzerResults.setTestName(mappedName.getOpenElisTestName());
            analyzerResults.setIsControl(false);
            results.add(analyzerResults);

            AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(analyzerResults);
            if (resultFromDB != null) {
                results.add(resultFromDB);
            }
        }
    }

    private String getDictioanryValueForResult(String result) {
        if ("NEG".equals(result)) {
            return NEGATIVE_DICTIONARY_ID;
        } else if ("REACTIVE".equals(result)) {
            return POSITIVE_DICTIONARY_ID;
        } else if ("*".equals(result)) {
            return INDETERMINATE_DICTIONARY_ID;
        }

        return null;
    }

    @Override
    public String getError() {
        return "Evolis analyzer unable to write to database";
    }
}
