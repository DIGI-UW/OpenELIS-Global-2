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

/**
 * @author benzd1 bug 2293 this compares objects that (AssignableTest) can
 *         contain tests and panels (combination of tests) it is used for Assign
 *         Test popups
 */
public class AssignableTestComparator implements Comparable {
    String name;

    // You can put the default sorting capability here
    public int compareTo(Object obj) {
        Test t = (Test) obj;
        return this.name.compareTo(TestServiceImpl.getUserLocalizedTestName(t));
    }

    public static final Comparator PANEL_TYPE_DESCRIPTION_COMPARATOR = new Comparator() {
        public int compare(Object a, Object b) {
            AssignableTest t_a = (AssignableTest) a;
            AssignableTest t_b = (AssignableTest) b;

            return ((t_a.getDescription().toLowerCase()).compareTo(t_b.getDescription().toLowerCase()));
        }
    };

    public static final Comparator TEST_TYPE_NAME_COMPARATOR = new Comparator() {
        public int compare(Object a, Object b) {
            AssignableTest t_a = (AssignableTest) a;
            AssignableTest t_b = (AssignableTest) b;

            return ((t_a.getAssignableTestName().toLowerCase()).compareTo(t_b.getAssignableTestName().toLowerCase()));
        }
    };

    public static final Comparator TEST_TYPE_DESCRIPTION_COMPARATOR = new Comparator() {
        public int compare(Object a, Object b) {
            AssignableTest t_a = (AssignableTest) a;
            AssignableTest t_b = (AssignableTest) b;

            return ((t_a.getDescription().toLowerCase()).compareTo(t_b.getDescription().toLowerCase()));
        }
    };
}
