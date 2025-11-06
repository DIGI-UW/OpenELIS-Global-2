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

package org.openelisglobal.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ObjectUtils;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.spring.util.SpringContext;

/**
 * This enum defines lists from the dictionary table based on dictionary
 * category name that are used for user selections, options, lookup etc. in the
 * UI for various demographic (observation) questions. To use the whole list in
 * a jsp the notation would be "dictionaryList.MARITAL_STATUS.list", or
 * "dictionaryList.MARITAL_STATUS.list[1]"
 *
 * @author pahill
 * @since 2010-04-14
 */
public enum ObservationHistoryList {
    EDUCATION_LEVELS("Education Level"), MARITAL_STATUSES("Marital Status"), NATIONALITIES("Nationality"),
    SIMPLIFIED_NATIONALITIES("Simplified Nationality"), YES_NO_UNKNOWN("Yes No Unknown"), YES_NO_NA("Yes No NA"),
    YES_NO("Yes No"), YES_NO_UNKNOWN_NA(YES_NO_NA, "Unknown", true),
    YES_NO_UNKNOWN_NA_NOTSPEC(YES_NO_UNKNOWN_NA, "NotSpeced", true), AIDS_STAGES("AIDS Stages"),
    HIV_STATUSES("HIVResult"), HIV_TYPES("HIV Status"),

    ARV_DISEASES("ARV Disease", false), ARV_DISEASES_SHORT(ARV_DISEASES, "DiarrheaC", false), /*
                                                                                               * same as ARV_DISEASES
                                                                                               * minus diarrhea
                                                                                               */
    ARV_PROPHYLAXIS("ARV Prophylaxis"), ARV_PROPHYLAXIS_2("Secondary Prophylaxis"), ARV_REGIME("ARV Treatment Regime"),
    ARV_REASON_FOR_VL_DEMAND("Reason for Viral Load Request", false),

    RTN_DISEASES("RTN Diseases", false), RTN_EXAM_DISEASES("RTN Examination Diseases", false),

    EID_WHICH_PCR("EID Which PCR Test"), EID_SECOND_PCR_REASON("Reason for Second PCR Test"),
    EID_TYPE_OF_CLINIC("EID Type of Clinic"), EID_HOW_CHILD_FED("EID How Infant Eating"),
    EID_STOPPED_BREASTFEEDING("EID Stopped Breastfeeding"), EID_MOTHERS_HIV_STATUS("Mother's HIV Status"),
    EID_MOTHERS_ARV_TREATMENT("Mother's ARV Treatment"), EID_INFANT_PROPHYLAXIS_ARV("EID Infant's ARV Prophylaxis"),
    EID_INFANT_COTRIMOXAZOLE("EID Infant Cotrimoxazole"),

    SPECIAL_REQUEST_REASONS("Special Request Reason"),

    REC_STATUS("REC_STATUS"), HPV_SAMPLING_METHOD("HPV Sampling Method"),;

    private DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);

    private static List<Dictionary> copyListAndDeleteEntry(List<Dictionary> oldList, String entryToDelete) {
        List<Dictionary> newList = new ArrayList<>(oldList.size());
        for (int i = 0; i < oldList.size(); i++) {
            if (!entryToDelete.equals(oldList.get(i).getLocalAbbreviation())) {
                newList.add(oldList.get(i));
            }
        }
        return newList;
    }

    private static List<Dictionary> copyListAndAddEntry(List<Dictionary> oldList, Dictionary entryToAdd) {
        List<Dictionary> newList = new ArrayList<>(oldList);
        if (ObjectUtils.isNotEmpty(entryToAdd)) {
            newList.add(entryToAdd);
        }
        return newList;
    }

    /**
     * The point of this map is provide a way to get lists needed in UI into a Map
     * so that on a JSP page, the caller can use: dropDowns.AIDS_STAGES.list
     */
    public static final Map<String, ObservationHistoryList> MAP = new HashMap<>();

    static {
        for (ObservationHistoryList ds : ObservationHistoryList.values()) {
            if (ObjectUtils.isNotEmpty(ds)) {
                MAP.put(ds.name(), ds);
            }
        }
    }

    /** The dictionary_category.category_name */
    private String listName;

    /** The cached list */
    private List<Dictionary> list;

    /** need to re-order in memory */
    private boolean orderByMessageResource;

    private List<String> idToIndex;
    /**
     * Deriving one list from another involves dropping or adding one entry. This is
     * the name (local_abbrev, but check the code below) of that entry.
     */
    private String entryTag = null;
    /** T = Add the entry named or F= drop the entry named */
    private boolean add;

    /**
     * A List holder for some category from the dictionary.
     *
     * @param name
     * @param orderByMessageResource - TRUE => by the localized string from the
     *                               message resource file; FALSE => DB ID order.
     */
    private ObservationHistoryList(String listName, boolean orderByMessageResource) {
        this.listName = listName;
        this.orderByMessageResource = orderByMessageResource;
    }

    /**
     * Simpler version of the above
     *
     * @param categoryName
     */
    private ObservationHistoryList(String categoryName) {
        this(categoryName, true);
    }

    /**
     * A List holder for some category from the dictionary.
     *
     * @param name this name of the list
     * @param
     */
    private ObservationHistoryList(String listName, String item, boolean add, boolean orderByMessageResource) {
        this.listName = listName + ((add) ? "+" : "-") + item;
        this.orderByMessageResource = orderByMessageResource;
        entryTag = item;
        this.add = add;
    }

    private ObservationHistoryList(ObservationHistoryList otherList, String item, boolean add) {
        this(otherList.getListName(), item, add, otherList.orderByMessageResource);
        list = otherList.getList();
        modifyList();
        // now we have everything in the list, but since the original Service call did
        // the ordering and we just added another entry, we need to re-sort.
        if (orderByMessageResource) {
            BaseObject.sortByLocalizedName(list);
        }
    }

    /**
     * the name of the list maybe the database name, but maybe not, if this is copy
     * modify of another list.
     *
     * @return
     */
    public String getListName() {
        return listName;
    }

    /**
     * The database list of all dictionary entites which have the same
     * dictionary_category where category is defined by the categoryName Lists are
     * lazy loaded as needed, so you can use the collection of such lists which
     * worrying that you'll waste time reading ones you are not using.
     *
     * @return
     */
    public List<Dictionary> getList() {
        if (list == null) {
            if (orderByMessageResource) {
                list = SpringContext.getBean(DictionaryService.class)
                        .getDictionaryEntrysByCategoryNameLocalizedSort(listName);
            } else {
                list = dictionaryService.getDictionaryEntrysByCategoryAbbreviation("categoryName", listName, false);
            }
            modifyList();
        }
        return list;
    }

    /**
     * Add/Drop any special entry to create just set of items requested in the
     * constructor.
     */
    private void modifyList() {
        if (entryTag != null) { // if any special case to add or remove
            if (add) {
                Dictionary d = new Dictionary();
                d.setLocalAbbreviation(entryTag);
                d = dictionaryService.getDictionaryByLocalAbbrev(d);
                list = copyListAndAddEntry(list, d);
            } else {
                list = copyListAndDeleteEntry(list, entryTag);
            }
        }
    }

    public int getIndexOfId(String id) {
        if (idToIndex == null) {
            idToIndex = new ArrayList<>();
            List<Dictionary> ds = getList();
            for (int i = 0; i < ds.size(); i++) {
                Dictionary d = ds.get(i);
                idToIndex.add(d.getId());
            }
        }
        return idToIndex.indexOf(id);
    }
}
