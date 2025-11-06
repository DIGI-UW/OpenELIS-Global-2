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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.reports.valueholder.resultsreport;

import java.util.Comparator;

/**
 * @author benzd1 bugzilla 2264
 */
public class ResultsReportAnalyteResultComparator implements Comparable {
    String name;

    // You can put the default sorting capability here
    public int compareTo(Object obj) {
        ResultsReportAnalyteResult analyteResult = (ResultsReportAnalyteResult) obj;
        return this.name.compareTo(getLongVersionOfNumber(analyteResult.getTestResultSortOrder()));
    }

    public static final Comparator VALUE_COMPARATOR = new Comparator() {
        public int compare(Object a, Object b) {
            ResultsReportAnalyteResult analyteResult_a = (ResultsReportAnalyteResult) a;
            ResultsReportAnalyteResult analyteResult_b = (ResultsReportAnalyteResult) b;

            // bugzilla 2184: handle null sort value
            String aValue = "0000000000000000000000";
            if (analyteResult_a != null && analyteResult_a.getTestResultSortOrder() != null) {
                aValue = getLongVersionOfNumber(analyteResult_a.getTestResultSortOrder());
            }

            String bValue = "0000000000000000000000";
            if (analyteResult_b != null && analyteResult_b.getTestResultSortOrder() != null) {
                bValue = getLongVersionOfNumber(analyteResult_b.getTestResultSortOrder());
            }
            return (aValue.compareTo(bValue));
        }
    };

    private static String getLongVersionOfNumber(String number) {
        String longVersion = "";
        if (number.length() < 22) {
            int zeroPaddingLength = 10 - number.length();
            StringBuffer zeros = new StringBuffer();
            for (int i = 0; i < zeroPaddingLength; i++) {
                zeros.append("0");
            }
            longVersion = zeros + number;
        }
        if (number.length() == 22) {
            longVersion = number;
        }

        return longVersion;
    }
}
