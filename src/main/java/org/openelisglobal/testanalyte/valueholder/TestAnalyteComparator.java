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
package org.openelisglobal.testanalyte.valueholder;

import java.util.Comparator;

public class TestAnalyteComparator implements Comparable {
    String name;

    // You can put the default sorting capability here
    public int compareTo(Object obj) {
        TestAnalyte ta = (TestAnalyte) obj;
        return this.name.compareTo(ta.getAnalyte().getAnalyteName());
    }

    public static final Comparator NAME_COMPARATOR = new Comparator() {
        public int compare(Object a, Object b) {
            TestAnalyte ta_a = (TestAnalyte) a;
            TestAnalyte ta_b = (TestAnalyte) b;

            return ((ta_a.getAnalyte().getAnalyteName()).toLowerCase()
                    .compareTo((ta_b.getAnalyte().getAnalyteName()).toLowerCase()));
        }
    };
}
