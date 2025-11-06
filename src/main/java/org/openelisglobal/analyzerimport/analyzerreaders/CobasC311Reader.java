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
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerimport.util.MappedTestName;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.util.DateUtil;

public class CobasC311Reader extends AnalyzerLineInserter {

    private static final double ROUND_UP_KICKER = 0.000001;
    private int ORDER_NUMBER = 0;
    private int ORDER_DATE = 0;
    private int ALTLIndex = 0;
    private int ASTLIndex = 0;
    private int creatininIndex = 0;
    private int glycemiaIndex = 0;

    private static final String ALTL_NAME = "ALTL";
    private static final String ASTL_NAME = "ASTL";
    private static final String CREATININ_NAME = "Creatinin";
    private static final String GLYCEMIA_NAME = "Glycemia";
    private static final String DELIMITER = ",";
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String VALID_PREFIXES = "LART,LDBS,LRTN,LIND,LSPE";

    private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();
    private String error;

    @Override
    public boolean insert(List<String> lines, String currentUserId) {
        error = null;
        boolean successful = true;

        List<AnalyzerResults> results = new ArrayList<>();

        boolean columnsFound = manageColumns(lines.get(0), lines.get(3));

        if (!columnsFound) {
            error = "Cobas C311 analyzer: Unable to find correct columns in file";
            return false;
        }

        for (int i = 4; i < lines.size(); ++i) {
            createAnalyzerResultFromLine(lines.get(i), results);
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

    private boolean manageColumns(String line_1, String line_3) {
        String[] fields = line_1.split(DELIMITER);

        for (int i = 0; i < fields.length; i++) {
            String header = fields[i].replace("\"", "");
            if ("685".equals(header)) {
                ALTLIndex = i - 1;
            } else if ("687".equals(header)) {
                ASTLIndex = i - 1;
            } else if ("690".equals(header)) {
                creatininIndex = i - 1;
            } else if ("767".equals(header)) {
                glycemiaIndex = i - 1;
            }
        }

        fields = line_3.split(DELIMITER);

        for (int i = 0; i < fields.length; i++) {
            String header = fields[i].replace("\"", "");
            if ("S_ID".equals(header)) {
                ORDER_NUMBER = i;
            } else if ("M_Date".equals(header)) {
                ORDER_DATE = i;
            }
        }

        return ORDER_DATE != 0 && ORDER_NUMBER != 0 && ALTLIndex != 0 && ASTLIndex != 0 && creatininIndex != 0
                && glycemiaIndex != 0;
    }

    private void addValueToResults(List<AnalyzerResults> resultList, AnalyzerResults result) {
        if (result != null) {
            resultList.add(result);

            AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(result);
            if (resultFromDB != null) {
                resultList.add(resultFromDB);
            }
        }
    }

    private void createAnalyzerResultFromLine(String line, List<AnalyzerResults> resultList) {
        String[] fields = line.split(DELIMITER);

        if (fields.length <= ORDER_DATE) {
            return;
        }

        String accessionNumber = fields[ORDER_NUMBER].trim();
        Timestamp orderTimeStamp = DateUtil.convertStringDateToTimestampWithPattern(fields[ORDER_DATE].trim(),
                DATE_PATTERN);

        addValueToResults(resultList,
                createAnalyzerResult(ALTL_NAME, fields, ALTLIndex, accessionNumber, orderTimeStamp));
        addValueToResults(resultList,
                createAnalyzerResult(ASTL_NAME, fields, ASTLIndex, accessionNumber, orderTimeStamp));
        addValueToResults(resultList,
                createAnalyzerResult(CREATININ_NAME, fields, creatininIndex, accessionNumber, orderTimeStamp));
        addValueToResults(resultList,
                createAnalyzerResult(GLYCEMIA_NAME, fields, glycemiaIndex, accessionNumber, orderTimeStamp));
    }

    private AnalyzerResults createAnalyzerResult(String analyzerTestName, String[] fields, int index,
            String accessionNumber, Timestamp orderTimeStamp) {

        if (fields.length <= index) {
            return null;
        }

        String result = fields[index].trim();

        if (GenericValidator.isBlankOrNull(result)) {
            return null;
        }

        AnalyzerResults analyzerResults = new AnalyzerResults();

        MappedTestName mappedName = AnalyzerTestNameCache.getInstance().getMappedTest(AnalyzerTestNameCache.COBAS_C311,
                analyzerTestName);

        if (mappedName == null) {
            mappedName = AnalyzerTestNameCache.getInstance().getEmptyMappedTestName(AnalyzerTestNameCache.COBAS_C311,
                    analyzerTestName);
        }

        analyzerResults.setAnalyzerId(mappedName.getAnalyzerId());
        analyzerResults.setResult(adjustResult(analyzerTestName, result));
        analyzerResults.setCompleteDate(orderTimeStamp);
        analyzerResults.setTestId(mappedName.getTestId());
        analyzerResults.setIsControl(
                accessionNumber.length() < 9 || !VALID_PREFIXES.contains(accessionNumber.subSequence(0, 3)));
        analyzerResults.setTestName(mappedName.getOpenElisTestName());
        analyzerResults.setResultType("N");

        analyzerResults.setAccessionNumber(accessionNumber);

        return analyzerResults;
    }

    private String adjustResult(String analyzerTestName, String result) {

        if (ALTL_NAME.equals(analyzerTestName)) {
            return String.valueOf(Math.rint(Double.parseDouble(result) + ROUND_UP_KICKER)).split("\\.")[0];
        }

        if (ASTL_NAME.equals(analyzerTestName)) {
            return String.valueOf(Math.rint(Double.parseDouble(result) + ROUND_UP_KICKER)).split("\\.")[0];
        }

        if (CREATININ_NAME.equals(analyzerTestName)) {
            return String.valueOf(Math.rint((Double.parseDouble(result) + ROUND_UP_KICKER) * 10.0)).split("\\.")[0];
        }

        if (GLYCEMIA_NAME.equals(analyzerTestName)) {
            return String.valueOf(Math.rint((Double.parseDouble(result) + ROUND_UP_KICKER) * 100) / 100);
        }

        return result;
    }

    @Override
    public String getError() {
        return error;
    }
}
