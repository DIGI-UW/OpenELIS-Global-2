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
public class ResultsReportSampleComparator implements Comparable {
    String name;

    // You can put the default sorting capability here
    public int compareTo(Object obj) {
        ResultsReportSample sample = (ResultsReportSample) obj;
        String concatenatedSortField = (getLongVersionOfNumber(sample.getOrganizationId())
                + getLongVersionOfNumber(sample.getAccessionNumber()));
        return this.name.compareTo(concatenatedSortField);
    }

    public static final Comparator VALUE_COMPARATOR = new Comparator() {
        public int compare(Object a, Object b) {
            ResultsReportSample sample_a = (ResultsReportSample) a;
            ResultsReportSample sample_b = (ResultsReportSample) b;

            String sample_aConcatenatedSortField = (getLongVersionOfNumber(sample_a.getOrganizationId())
                    + getLongVersionOfNumber(sample_a.getAccessionNumber()));
            String sample_bConcatenatedSortField = (getLongVersionOfNumber(sample_b.getOrganizationId())
                    + getLongVersionOfNumber(sample_b.getAccessionNumber()));

            return ((sample_aConcatenatedSortField).compareTo((sample_bConcatenatedSortField)));
        }
    };

    private static String getLongVersionOfNumber(String number) {
        String longVersion = "";
        if (number.length() < 10) {
            int zeroPaddingLength = 10 - number.length();
            StringBuffer zeros = new StringBuffer();
            for (int i = 0; i < zeroPaddingLength; i++) {
                zeros.append("0");
            }
            longVersion = zeros + number;
        }
        if (number.length() == 10) {
            longVersion = number;
        }

        return longVersion;
    }
}
