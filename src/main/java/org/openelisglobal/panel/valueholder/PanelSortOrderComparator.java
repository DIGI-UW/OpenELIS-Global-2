/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.panel.valueholder;

import java.util.Comparator;

public class PanelSortOrderComparator implements Comparable<Object> {
    String sortOrder;

    public int compareTo(Object obj) {
        Panel t = (Panel) obj;
        return this.sortOrder.compareTo(t.getSortOrder());
    }

    public static final Comparator<Object> SORT_ORDER_COMPARATOR = new Comparator<Object>() {
        public int compare(Object a, Object b) {
            Panel t_a = (Panel) a;
            Panel t_b = (Panel) b;
            return (new Integer(t_a.getSortOrderInt())).compareTo(new Integer(t_b.getSortOrderInt()));
        }
    };
}
