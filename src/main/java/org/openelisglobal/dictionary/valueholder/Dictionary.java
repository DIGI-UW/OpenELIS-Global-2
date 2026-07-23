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
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Comparator;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.localization.valueholder.Localization;

@Entity
@Table(name = "DICTIONARY")
@DynamicUpdate
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class Dictionary extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    public class ComparatorLocalizedName implements Comparator<Dictionary> {
        @Override
        public int compare(Dictionary o1, Dictionary o2) {
            return o1.getLocalizedName().compareTo(o2.getDefaultLocalizedName());
        }
    }

    @Id
    @Column(name = "ID", precision = 10, scale = 0)
    @GeneratedValue(generator = "dictionary_seq_gen")
    @GenericGenerator(name = "dictionary_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "dictionary_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @Column(name = "IS_ACTIVE", length = 1)
    private String isActive;

    @Column(name = "DICT_ENTRY", length = 4000)
    private String dictEntry;

    @Transient
    private String selectedDictionaryCategoryId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "DICTIONARY_CATEGORY_ID")
    private DictionaryCategory dictionaryCategory;

    @Column(name = "LOCAL_ABBREV", length = 10)
    private String localAbbreviation;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "display_key", length = 60)
    private String nameKey;

    @Column(name = "LOINC_CODE", length = 20)
    private String loincCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "name_localization_id")
    private Localization localizedDictionaryName;

    public Dictionary() {
        super();
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getIsActive() {
        return this.isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public DictionaryCategory getDictionaryCategory() {
        return this.dictionaryCategory;
    }

    public void setDictionaryCategory(DictionaryCategory dictionaryCategory) {
        this.dictionaryCategory = dictionaryCategory;
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

    @Override
    public String getNameKey() {
        return nameKey;
    }

    @Override
    public void setNameKey(String nameKey) {
        this.nameKey = nameKey;
    }

    public String getLoincCode() {
        return loincCode;
    }

    public void setLoincCode(String loincCode) {
        this.loincCode = loincCode;
    }

    public Localization getLocalizedDictionaryName() {
        return localizedDictionaryName;
    }

    public void setLocalizedDictionaryName(Localization localizedDictionaryName) {
        this.localizedDictionaryName = localizedDictionaryName;
    }

    public String getDisplayValue() {
        if (localizedDictionaryName == null) {
            return getDictEntry();
        } else {
            return localizedDictionaryName.getLocalizedValue();
        }
    }

    /**
     * Override to prioritize database localization over message bundle
     * (display_key). Order of precedence: 1. Database localization
     * (localization_value table) 2. Message bundle lookup via nameKey/display_key
     * 3. dictEntry as final fallback
     */
    @Override
    @JsonIgnore
    public String getLocalizedName() {
        // First, try the new database localization system
        if (localizedDictionaryName != null) {
            String localizedValue = localizedDictionaryName.getLocalizedValue();
            if (localizedValue != null && !localizedValue.isEmpty()) {
                return localizedValue;
            }
        }

        // Fall back to message bundle lookup via nameKey (display_key column)
        String key = getNameKey();
        if (key != null) {
            String localizedName = MessageUtil.getContextualMessage(key.trim());
            if (localizedName != null && !localizedName.equals(key.trim())) {
                return localizedName;
            }
        }

        // Final fallback to dictEntry
        return dictEntry;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return dictEntry;
    }

    @Override
    public String toString() {
        return "Dictionary [id=" + id + ", localAbbreviation=" + localAbbreviation + ", nameKey=" + getNameKey() + "]";
    }
}
