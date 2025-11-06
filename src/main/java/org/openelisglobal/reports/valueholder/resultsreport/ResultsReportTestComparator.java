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
import org.openelisglobal.common.util.StringUtil;

/**
 * @author benzd1 bugzilla 2264
 */
public class ResultsReportTestComparator implements Comparable<ResultsReportTest> {
    String name;

    // You can put the default sorting capability here
    @Override
    public int compareTo(ResultsReportTest test) {
        return this.name.compareTo(test.getTestDescription());
    }

    public static final Comparator<ResultsReportTest> NAME_COMPARATOR = new Comparator<ResultsReportTest>() {
        @Override
        public int compare(ResultsReportTest a, ResultsReportTest b) {

            // bugzilla 2184: handle null sort value
            String aValue = "";
            if (a != null && a.getTestDescription() != null) {
                aValue = a.getTestDescription();
            }

            String bValue = "";
            if (b != null && b.getTestDescription() != null) {
                bValue = b.getTestDescription();
            }
            return (aValue.toLowerCase().compareTo(bValue.toLowerCase()));
        }
    };

    // bugzilla 1856
    public static final Comparator<ResultsReportTest> SORT_ORDER_COMPARATOR = new Comparator<ResultsReportTest>() {
        @Override
        public int compare(ResultsReportTest a, ResultsReportTest b) {
            String aValue = a.getAnalysis().getTest().getSortOrder();
            String bValue = b.getAnalysis().getTest().getSortOrder();

            if (StringUtil.isNullorNill(aValue)) {
                aValue = "0";
            }

            if (StringUtil.isNullorNill(bValue)) {
                bValue = "0";
            }

            return (aValue.compareTo(bValue));
        }
    };
}
