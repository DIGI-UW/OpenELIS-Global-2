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

public class CobasTaqmanReader extends AnalyzerLineInserter {

    private static final String UNDER_THREASHOLD = "< LL";
    private static final double THREASHOLD = 20.0;

    @SuppressWarnings("unused")
    private static final int PATIENT_NAME = 0;

    @SuppressWarnings("unused")
    private static final int PATIENT_ID = 1;

    @SuppressWarnings("unused")
    private static final int ORDER_NUMBER = 2;

    private static final int ORDER_DATE = 3;
    private static final int SAMPLE_ID = 4;
    private static final int SAMPLE_TYPE = 5;

    @SuppressWarnings("unused")
    private static final int BATCH_ID = 6;

    private static final int TEST = 7;
    private static final int RESULT = 8;
    private static final int UNIT = 9;

    private static final String DELIMITER = "\\t";
    private static final String ALT_DELIMITER = ",";
    private static final String DATE_PATTERN = "yyyy/MM/dd HH:mm:ss";
    private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();

    @Override
    public boolean insert(List<String> lines, String currentUserId) {

        boolean successful = true;

        List<AnalyzerResults> results = new ArrayList<>();

        for (int i = 1; i < lines.size(); ++i) {
            createAnalyzerResultFromLine(lines.get(i), results);
        }

        if (results.size() > 0) {

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

    private void createAnalyzerResultFromLine(String line, List<AnalyzerResults> resultList) {
        String[] fields = line.split(ALT_DELIMITER);
        if (fields.length < 5) {
            fields = line.split(DELIMITER);
        }

        AnalyzerResults analyzerResults = new AnalyzerResults();
        MappedTestName mappedName = AnalyzerTestNameCache.getInstance()
                .getMappedTest(AnalyzerTestNameCache.COBAS_TAQMAN, fields[TEST].replace("\"", "").trim());

        if (mappedName == null) {
            mappedName = AnalyzerTestNameCache.getInstance().getEmptyMappedTestName(AnalyzerTestNameCache.COBAS_TAQMAN,
                    fields[TEST].replace("\"", "").trim());
        }

        String result = getAppropriateResults(fields[RESULT]);
        analyzerResults.setAnalyzerId(mappedName.getAnalyzerId());
        analyzerResults.setResult(result);
        analyzerResults.setUnits(UNDER_THREASHOLD.equals(result) ? "" : fields[UNIT].replace("\"", "").trim());
        analyzerResults.setCompleteDate(DateUtil
                .convertStringDateToTimestampWithPattern(fields[ORDER_DATE].replace("\"", "").trim(), DATE_PATTERN));
        analyzerResults.setAccessionNumber(fields[SAMPLE_ID].replace("\"", "").trim());
        analyzerResults.setTestId(mappedName.getTestId());
        analyzerResults.setIsControl(!"S".equals(fields[SAMPLE_TYPE].replace("\"", "").trim()));
        analyzerResults.setTestName(mappedName.getOpenElisTestName());
        analyzerResults.setResultType("A");

        addValueToResults(resultList, analyzerResults);
    }

    private String getAppropriateResults(String result) {
        result = result.replace("\"", "").trim();
        if ("Target Not Detected".equalsIgnoreCase(result) || "Below range".equalsIgnoreCase(result)) {
            result = UNDER_THREASHOLD;
        } else {

            String workingResult = result.split("\\(")[0].replace("<", "").replace("E", "");
            String[] splitResult = workingResult.split("\\+");

            try {
                Double resultAsDouble = Double.parseDouble(splitResult[0])
                        * Math.pow(10, Double.parseDouble(splitResult[1]));

                if (resultAsDouble <= THREASHOLD) {
                    result = UNDER_THREASHOLD;
                } else {
                    result = String.valueOf((int) (Math.round(resultAsDouble))) + result.substring(result.indexOf("("));
                }
            } catch (NumberFormatException e) {
                return "XXXX";
            }
        }

        return result;
    }

    @Override
    public String getError() {
        return "Cobas Taqman unable to write to database";
    }
}
