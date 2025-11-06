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
package org.openelisglobal.organization.valueholder;

import java.util.Comparator;

/**
 * @author AIS bug 1719
 */
public class OrganizationComparator implements Comparable<Organization> {
    String name;

    // You can put the default sorting capability here
    public int compareTo(Organization that) {
        return this.name.compareTo(that.getName());
    }

    public static final Comparator<Organization> NAME_COMPARATOR = new Comparator<Organization>() {
        public int compare(Organization a, Organization b) {
            Organization c_a = (Organization) a;
            Organization c_b = (Organization) b;

            return ((c_a.getId().toLowerCase()).compareTo((c_b.getId().toLowerCase())));
        }
    };

    public static final Comparator<Organization> SHORTNAME_NUMERIC_COMPARATOR = new Comparator<Organization>() {
        public int compare(Organization o1, Organization o2) {
            try {
                Integer num1 = Integer.valueOf(o1.getShortName());
                Integer num2 = Integer.valueOf(o2.getShortName());
                return num1.compareTo(num2);
            } catch (NumberFormatException e) {
                return o1.getShortName().compareTo(o2.getShortName());
            }
        }
    };

    /**
     * The NAME_COMPARATOR claims to compare by name, but it actually looks at ID
     * (and its used). This one does it by the OrganizationName field.
     */
    public static final Comparator<Organization> ORGANIZATION_NAME_COMPARATOR = new Comparator<Organization>() {
        public int compare(Organization o1, Organization o2) {
            return o1.getOrganizationName().compareTo(o2.getOrganizationName());
        }
    };
}
