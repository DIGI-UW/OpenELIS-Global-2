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
import org.openelisglobal.test.service.TestServiceImpl;

public class TestComparator implements Comparable<Test> {
    String name;

    // You can put the default sorting capability here
    public int compareTo(Test t) {
        return this.name.compareTo(TestServiceImpl.getUserLocalizedTestName(t));
    }

    public static final Comparator<Test> NAME_COMPARATOR = new Comparator<Test>() {
        public int compare(Test a, Test b) {
            return ((TestServiceImpl.getUserLocalizedTestName(a).toLowerCase())
                    .compareTo(TestServiceImpl.getUserLocalizedTestName(b).toLowerCase()));
        }
    };

    public static final Comparator<Test> DESCRIPTION_COMPARATOR = new Comparator<Test>() {
        public int compare(Test a, Test b) {
            return ((TestServiceImpl.getLocalizedTestNameWithType(a).toLowerCase())
                    .compareTo(TestServiceImpl.getLocalizedTestNameWithType(b).toLowerCase()));
        }
    };
}
