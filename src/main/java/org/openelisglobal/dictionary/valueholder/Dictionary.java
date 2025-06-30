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
package org.openelisglobal.dictionary.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Comparator;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.openelisglobal.localization.valueholder.Localization;

public class Dictionary extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String loincCode;

    public class ComparatorLocalizedName implements Comparator<Dictionary> {
        @Override
        public int compare(Dictionary o1, Dictionary o2) {
            return o1.getLocalizedName().compareTo(o2.getDefaultLocalizedName());
        }
    }

    private String id;

    private String isActive;

    private String dictEntry;

    private String selectedDictionaryCategoryId;

    private ValueHolderInterface dictionaryCategory;

    private String localAbbreviation;

    private Integer sortOrder;

    private ValueHolder localizedDictionaryName;

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getLocalAbbreviation() {
        return localAbbreviation;
    }

    public void setLocalAbbreviation(String localAbbreviation) {
        this.localAbbreviation = localAbbreviation;
    }

    public Dictionary() {
        super();
        this.dictionaryCategory = new ValueHolder();
        this.localizedDictionaryName = new ValueHolder();
    }

    @Override
    public String getId() {
        return this.id;
    }

    public String getIsActive() {
        return this.isActive;
    }

    public DictionaryCategory getDictionaryCategory() {
        return (DictionaryCategory) this.dictionaryCategory.getValue();
    }

    public void setDictionaryCategory(DictionaryCategory dictionaryCategory) {
        this.dictionaryCategory.setValue(dictionaryCategory);
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public String getDictEntry() {
        return dictEntry;
    }

    public void setDictEntry(String dictEntry) {
        this.dictEntry = dictEntry;
    }

    @JsonIgnore
    public String getDictEntryDisplayValue() {
        String dictEntryDisplayValue;
        if (!StringUtil.isNullorNill(this.localAbbreviation)) {

            dictEntryDisplayValue = localAbbreviation + IActionConstants.LOCAL_CODE_DICT_ENTRY_SEPARATOR_STRING
                    + dictEntry;
        } else {
            dictEntryDisplayValue = dictEntry;
        }
        return dictEntryDisplayValue;
    }

    public String getSelectedDictionaryCategoryId() {
        return selectedDictionaryCategoryId;
    }

    public void setSelectedDictionaryCategoryId(String selectedDictionaryCategoryId) {
        this.selectedDictionaryCategoryId = selectedDictionaryCategoryId;
    }

    public String getDisplayValue() {
        if (localizedDictionaryName == null || localizedDictionaryName.getValue() == null) {
            return getDictEntry();
        } else {
            return getLocalizedDictionaryName().getLocalizedValue();
        }
    }

    public Localization getLocalizedDictionaryName() {
        return (Localization) localizedDictionaryName.getValue();
    }

    public void setLocalizedDictionaryName(Localization localizedDictionaryName) {
        this.localizedDictionaryName.setValue(localizedDictionaryName);
    }

    @Override
    protected String getDefaultLocalizedName() {
        return dictEntry;
    }

    @Override
    public String toString() {
        return "Dictionary [id=" + id + ", localAbbreviation=" + localAbbreviation + ", nameKey=" + getNameKey() + "]";
    }

    public String getLoincCode() {
        return loincCode;
    }

    public void setLoincCode(String loincCode) {
        this.loincCode = loincCode;
    }

}
