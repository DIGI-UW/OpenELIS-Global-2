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

@SuppressWarnings("unused")
public class FacscaliburReader extends AnalyzerLineInserter {

    private static final String CONTROL_ACCESSION_PREFIX = "IMM";

    private static int index = 0;
    private static final int Institution = index++;
    private static final int Director = index++;
    private static final int Operator = index++;
    private static final int Cytometer = index++;
    private static final int Cytometer_Serial_Number = index++;
    private static final int Software_Version = index++;
    private static final int Sample_Name = index++;
    private static final int Sample_ID = index++;
    private static final int Case_Number = index++;
    private static final int Column_1 = index++;
    private static final int Column_2 = index++;
    private static final int Column_3 = index++;
    private static final int Panel_Name = index++;
    private static final int Collection_Date = index++;
    private static final int Date_Analyzed = index++;
    private static final int Lab_Report_File_Name = index++;
    private static final int Physicians_Report_File_Name = index++;
    private static final int WBC_Count_x1000 = index++;
    private static final int Lymphs_per = index++;
    private static final int Lymphs_x1000 = index++;
    private static final int Abs_Cnt_Bead_Name = index++;
    private static final int Abs_Cnt_Bead_Lot_ID = index++;
    private static final int Abs_Cnt_Beads_Pellet = index++;
    private static final int Control_Bead_Name = index++;
    private static final int Control_Bead_Lot_ID = index++;
    private static final int Control_Low_Beads_µL = index++;
    private static final int Control_Medium_Beads_µL = index++;
    private static final int Control_High_Beads_µL = index++;
    private static final int Control_Low_SD = index++;
    private static final int Control_Medium_SD = index++;
    private static final int Control_High_SD = index++;
    private static final int Ref_Range = index++;
    private static final int Comments = index++;
    private static final int Lymphosum = index++;
    private static final int CD3_per_Lymph_Difference = index++;
    private static final int T_Helper_Suppressor_Ratio = index++;
    private static final int Min_CD3_Abs_Cnt_Range = index++;
    private static final int Max_CD3_Abs_Cnt_Range = index++;
    private static final int Avg_CD3_CD4_per_T_Lymph = index++;
    private static final int Avg_CD3_CD8__per_T_Lymph = index++;
    private static final int Avg_CD3_CD4_CD8__per_T_Lymph = index++;
    private static final int Avg_CD3__per_Lymph = index++;
    private static final int Avg_CD3__Abs_Cnt = index++;
    private static final int Avg_CD3_CD4__per_Lymph = index++;
    private static final int Avg_CD3_CD4__Abs_Cnt = index++;
    private static final int Avg_CD3_CD8__per_Lymph = index++;
    private static final int Avg_CD3_CD8__Abs_Cnt = index++;
    private static final int Avg_CD3_CD4_CD8__per_Lymph = index++;
    private static final int Avg_CD3_CD4_CD8__Abs_Cnt = index++;
    private static final int Avg_CD19__per_Lymph = index++;
    private static final int Avg_CD19__Abs_Cnt = index++;
    private static final int Avg_CD16_56__per_Lymph = index++;
    private static final int Avg_CD16_56__Abs_Cnt = index++;
    private static final int CD45__Abs_Cnt = index++;
    private static final int CD3_CD4_CD45_FCS_File_Name = index++;
    private static final int CD3_CD4_CD45_Lot_ID = index++;
    private static final int CD3_CD4_CD45_Collection_Time = index++;
    private static final int CD3_CD4_CD45_Total_Events = index++;
    private static final int CD3_CD4_CD45_Tube_Name = index++;
    private static final int CD3_CD4_CD45_Attr_Def_File_Name = index++;
    private static final int CD3_CD4_CD45_Error_Codes = index++;
    private static final int CD3_CD4_CD45_Lymph_Events = index++;
    private static final int CD3_CD4_CD45_CD3__per_Lymph = index++;
    private static final int CD3_CD4_CD45_CD3__Abs_Cnt = index++;
    private static final int CD3_CD4_CD45_CD3_CD4__per_Lymph = index++;
    private static final int columns = index++;

    private static final String DELIMITER = "\t";

    private static final String DATE_PATTERN = "dd-MMMM-yy kk:mm:ss";
    private static final String ALT_DATE_PATTERN = "dd MMM yyyy KK:mm";
    private static String[] testNameIndex = new String[columns];
    private static String[] unitsIndex = new String[columns];

    {
        testNameIndex[Avg_CD3__per_Lymph] = "CD3_PER";
        testNameIndex[Avg_CD3_CD4__per_Lymph] = "CD4_PER";

        unitsIndex[Avg_CD3__per_Lymph] = "%";
        unitsIndex[Avg_CD3_CD4__per_Lymph] = "%";
    }

    @Override
    public boolean insert(List<String> lines, String currentUserId) {

        boolean successful = true;

        List<AnalyzerResults> results = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            addAnalyzerResultFromLine(results, lines.get(i));
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

    private void addAnalyzerResultFromLine(List<AnalyzerResults> results, String line) {
        String[] fields = line.split(DELIMITER);

        AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();
        String analyzerAccessionNumber = fields[Sample_ID];

        String date = fields[Collection_Date];

        for (int i = 0; i < testNameIndex.length; i++) {
            if (!GenericValidator.isBlankOrNull(testNameIndex[i])) {
                MappedTestName mappedName = AnalyzerTestNameCache.getInstance()
                        .getMappedTest(AnalyzerTestNameCache.FACSCALIBUR, testNameIndex[i]);

                if (mappedName == null) {
                    mappedName = AnalyzerTestNameCache.getInstance()
                            .getEmptyMappedTestName(AnalyzerTestNameCache.FACSCALIBUR, testNameIndex[i]);
                }

                AnalyzerResults analyzerResults = new AnalyzerResults();

                analyzerResults.setAnalyzerId(mappedName.getAnalyzerId());

                String result = fields[i];

                analyzerResults.setResult(result);
                analyzerResults.setUnits(unitsIndex[i]);
                String dateTime = date;
                Timestamp timestamp = getTimestampFromDate(dateTime);
                analyzerResults.setCompleteDate(timestamp);
                analyzerResults.setTestId(mappedName.getTestId());
                analyzerResults.setAccessionNumber(analyzerAccessionNumber);
                analyzerResults.setTestName(mappedName.getOpenElisTestName());

                if (analyzerAccessionNumber != null) {
                    analyzerResults.setIsControl(analyzerAccessionNumber.startsWith(CONTROL_ACCESSION_PREFIX));
                } else {
                    analyzerResults.setIsControl(false);
                }

                results.add(analyzerResults);

                AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(analyzerResults);
                if (resultFromDB != null) {
                    results.add(resultFromDB);
                }
            }
        }
    }

    private Timestamp getTimestampFromDate(String dateTime) {
        // Mixed date formats are sneaking in ie 24-Jul-12 15:07:09 or Mer 25 juil 2012
        // 9:46
        if (dateTime.contains("-")) {
            return DateUtil.convertStringDateToTimestampWithPatternNoLocale(dateTime, DATE_PATTERN);
        }

        String[] dateSegs = dateTime.split(" ");
        String month = getCorrectedMonth(dateSegs[2]);

        String date = dateSegs[1] + " " + month + " " + dateSegs[3] + " " + dateSegs[4];

        return DateUtil.convertStringDateToTimestampWithPattern(date, ALT_DATE_PATTERN);
    }

    // The analyzer is sending abbreviated months without the '.'
    private String getCorrectedMonth(String month) {
        if (month.endsWith(".") || month.equals("mars") || month.equals("mai") || month.equals("juin")
                || month.equals("août")) {
            return month;
        }

        return month + ".";
    }

    @Override
    public String getError() {
        return "Facscalibur analyzer unable to write to database";
    }
}
