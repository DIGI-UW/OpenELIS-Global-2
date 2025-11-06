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
package org.openelisglobal.test.valueholder;

import java.util.Comparator;

public class TestSectionComparator implements Comparable {
    String name;

    // You can put the default sorting capability here
    public int compareTo(Object obj) {
        TestSection ts = (TestSection) obj;
        return this.name.compareTo(ts.getTestSectionName());
    }

    public static final Comparator NAME_COMPARATOR = new Comparator() {
        public int compare(Object a, Object b) {
            TestSection ts_a = (TestSection) a;
            TestSection ts_b = (TestSection) b;

            return (((ts_a.getTestSectionName()).toLowerCase()).compareTo(((ts_b.getTestSectionName()).toLowerCase())));
        }
    };
}
