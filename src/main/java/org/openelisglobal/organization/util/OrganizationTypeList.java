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
 * Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
 *
 * Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.organization.util;

import static org.openelisglobal.organization.valueholder.OrganizationComparator.ORGANIZATION_NAME_COMPARATOR;
import static org.openelisglobal.organization.valueholder.OrganizationComparator.SHORTNAME_NUMERIC_COMPARATOR;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Well defined lists of organizations group by organization types and ordered
 * by some column of choice.
 *
 * @author pahill
 * @since 2010-05-19
 */
public enum OrganizationTypeList {
    // ARV_ORGS("shortName", null, "ARV Service Loc"),
    // ARV_ORGS_BY_NAME("organizationName", null, "ARV Service Loc"),
    ARV_ORGS("shortName", SHORTNAME_NUMERIC_COMPARATOR, "ARV Service Loc"),
    ARV_ORGS_BY_NAME("organizationName", ORGANIZATION_NAME_COMPARATOR, "ARV Service Loc"),
    EID_ORGS("shortName", SHORTNAME_NUMERIC_COMPARATOR, "EID ACONDA-VS CI", "EID EGPAF", "EID ESTHER", "EID ICAP",
            "SEV-CI", "ARIEL"),
    EID_ORGS_BY_NAME("organizationName", ORGANIZATION_NAME_COMPARATOR, "EID ACONDA-VS CI", "EID EGPAF", "EID ESTHER",
            "EID ICAP", "SEV-CI", "ARIEL"),
    RTN_HOSPITALS("shortName", ORGANIZATION_NAME_COMPARATOR, "RTN HIV Hospitals"),
    RTN_SERVICES("shortName", ORGANIZATION_NAME_COMPARATOR, "RTN HIV Service Loc"),
// RTN_HOSPITALS("shortName", SHORTNAME_NUMERIC_COMPARATOR, "RTN HIV
// Hospitals"),
// RTN_SERVICES("shortName", null, "RTN HIV Service Loc"),
    ;

    /**
     * Each member of the enum is one object which when asked will load a list based
     * on an OrganizationType.short_name
     *
     * @param comparator how to sort the list
     * @param name[]     oen or more organization types to use to find this list
     */
    private OrganizationTypeList(String orderBy, Comparator<Organization> comparator, String... name) {
        this.comparator = comparator;
        this.orderBy = orderBy;
        this.name = name;
    }

    /**
     * The point of this map is to provide a way to get lists needed in UI (or
     * reporting) into a Map so that on a JSP page, the caller can use:
     * dropDowns.AIDS_STAGES.list
     */
    public static final Map<String, OrganizationTypeList> MAP = new HashMap<>();

    static {
        for (OrganizationTypeList ds : OrganizationTypeList.values()) {
            MAP.put(ds.name(), ds);
        }
    }

    private String[] name;
    private String orderBy;
    private Comparator<Organization> comparator = null;

    /**
     * Each lists is loaded as needed.
     *
     * @return a list of organization associated with a particular organization type
     *         or an empty list.
     */
    public final List<Organization> getList() {
        List<Organization> all = SpringContext.getBean(OrganizationService.class).getOrganizationsByTypeName(orderBy,
                name);
        try {
            if (comparator != null) {
                Collections.sort(all, comparator);
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            // all = new ArrayList<>(); must not return empty List if sorting fails
        }
        return all;
    }
}
