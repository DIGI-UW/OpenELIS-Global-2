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
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.dictionary.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.dictionary.valueholder.Dictionary;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface DictionaryDAO extends BaseDAO<Dictionary, String> {

    // public boolean insertData(Dictionary dictionary) throws LIMSRuntimeException;

    // public List getAllDictionarys() throws LIMSRuntimeException;

    // public List getPageOfDictionarys(int startingRecNo) throws
    // LIMSRuntimeException;

    public void getData(Dictionary dictionary) throws LIMSRuntimeException;

    // public void updateData(Dictionary dictionary, boolean
    // isDictionaryFrozenCheckRequired) throws
    // LIMSRuntimeException;

    //

    /**
     * Finds some dictionary values where
     *
     * @param filter             -- the dictEntry column starts with this string
     *                           (case insensitive).
     * @param dictionaryCategory -- local_abbrev is this value
     * @return a list of Dictionary beans.
     * @throws LIMSRuntimeException
     */
    public List<Dictionary> getDictionaryEntrysByCategoryAbbreviation(String filter, String dictionaryCategory)
            throws LIMSRuntimeException;

    // bugzilla 2063
    /**
     * A list of Dictionary entries that belong to the same dictionary category
     * found by category name and ordered by the resource in the message file.
     * Numerically if the resource starts with an number; alphabetically, if not.
     *
     * @param categoryName the name to match
     * @return a list of matches
     * @throws LIMSRuntimeException
     */
    // public List<Dictionary> getDictionaryEntrysByCategoryAbbreviation(String
    // dictionaryCategory)
    // throws LIMSRuntimeException;

    /**
     * Find Dictionary entites by DictionaryCategory.categoryName, sorted by
     * Dictionary.getLocalizedName (Resource String)
     *
     * @param the dictionaryCategory.Name to match
     * @return List<Dictionary>
     * @throws LIMSRuntimeException
     */
    public List<Dictionary> getDictionaryEntrysByCategoryNameLocalizedSort(String dictionaryCategoryName)
            throws LIMSRuntimeException;

    /**
     * A more complex lower level version of getting entries when you want to find
     * them by some field and maybe sort by entry value.
     *
     * @param fieldName        - the bean name of the field to match
     * @param fieldValue       - value to match with fieldName
     * @param orderByDictEntry - what to order by TRUE => dictEntry field, FALSE =>
     *                         id. Either will be in ascending order.
     * @return a list of Dictionary items.
     * @throws LIMSRuntimeException
     */
    public List<Dictionary> getDictionaryEntrysByCategoryAbbreviation(String fieldName, String fieldValue,
            boolean orderByDictEntry);

    // bugzilla 1411
    // public Integer getTotalDictionaryCount() throws LIMSRuntimeException;

    // bugzilla 1367
    // public Dictionary getDictionaryByDictEntry(Dictionary dictionary, boolean
    // ignoreCase) throws
    // LIMSRuntimeException;

    // public Dictionary getDictionaryByLocalAbbrev(Dictionary dictionary) throws
    // LIMSRuntimeException;

    // bugzilla 1413
    // public List getPagesOfSearchedDictionarys(int startRecNo, String
    // searchString) throws
    // LIMSRuntimeException;

    // bugzilla 1413
    // public Integer getTotalSearchedDictionaryCount(String searchString) throws
    // LIMSRuntimeException;

    // public List<Dictionary> getDictionaryEntriesByCategoryId(String categoryId)
    // throws
    // LIMSRuntimeException;

    public Dictionary getDictionaryById(String dictionaryId) throws LIMSRuntimeException;

    // public Dictionary getDictionaryEntrysByNameAndCategoryDescription(String
    // dictionaryName, String
    // categoryDescription)
    // throws LIMSRuntimeException;

    // public Dictionary getDictionaryByDictEntry(String dictEntry) throws
    // LIMSRuntimeException;

    public Dictionary getDataForId(String dictId) throws LIMSRuntimeException;

    public boolean duplicateDictionaryExists(Dictionary dictionary);

    public boolean isDictionaryFrozen(Dictionary dictionary);
}
