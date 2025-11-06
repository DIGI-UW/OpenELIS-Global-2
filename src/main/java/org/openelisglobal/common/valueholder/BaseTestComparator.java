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
package org.openelisglobal.common.valueholder;

import java.util.Comparator;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.util.StringUtil;

/**
 * @author Benzd1 bugzilla 1856
 */
public class BaseTestComparator implements Comparable {
    String name;

    // You can put the default sorting capability here
    public int compareTo(Object obj) {
        Analysis analysis = (Analysis) obj;
        return this.name.compareTo(analysis.getTest().getSortOrder());
    }

    public static final Comparator SORT_ORDER_COMPARATOR = new Comparator() {
        public int compare(Object a, Object b) {
            Analysis analysis_a = (Analysis) a;
            Analysis analysis_b = (Analysis) b;
            String aValue = analysis_a.getTest().getSortOrder();
            String bValue = analysis_b.getTest().getSortOrder();

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
