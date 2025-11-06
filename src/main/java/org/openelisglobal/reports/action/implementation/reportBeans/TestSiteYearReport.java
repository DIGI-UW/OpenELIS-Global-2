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
package org.openelisglobal.reports.action.implementation.reportBeans;

import java.util.HashMap;
import java.util.Map;

public class TestSiteYearReport {
    public static enum Months {
        jan(0), feb(1), march(2), april(3), may(4), june(5), july(6), aug(7), sept(8), oct(9), nov(10), dec(11);

        private final int index;

        Months(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }

    private String testName;
    private String siteName;
    private Map<Months, Integer> monthCountMap = new HashMap<Months, Integer>();

    public TestSiteYearReport() {
        for (Months month : Months.values()) {
            monthCountMap.put(month, 0);
        }
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public Map<Months, Integer> getMonthCountMap() {
        return monthCountMap;
    }

    public void setMonthCountMap(Map<Months, Integer> monthCountMap) {
        this.monthCountMap = monthCountMap;
    }

    public void addToMonth(Months month, int newCount) {
        Integer count = monthCountMap.get(month);
        monthCountMap.put(month, count + newCount);
    }

    public int getCountForMonth(Months month) {
        return monthCountMap.get(month);
    }
}
