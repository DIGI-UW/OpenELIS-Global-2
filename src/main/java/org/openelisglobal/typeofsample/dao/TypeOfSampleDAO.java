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
package org.openelisglobal.typeofsample.dao;

import java.util.List;
import java.util.Locale;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface TypeOfSampleDAO extends BaseDAO<TypeOfSample, String> {

    public enum SampleDomain {
        HUMAN, ANIMAL, ENVIRONMENTAL
    }

    String getNameForTypeOfSampleId(String id);

    // public boolean insertData(TypeOfSample typeOfSample) throws
    // LIMSRuntimeException;

    // public void deleteData(List typeOfSamples) throws LIMSRuntimeException;

    List<TypeOfSample> getAllTypeOfSamples() throws LIMSRuntimeException;

    List<TypeOfSample> getAllTypeOfSamplesSortOrdered() throws LIMSRuntimeException;

    List<TypeOfSample> getPageOfTypeOfSamples(int startingRecNo) throws LIMSRuntimeException;

    void getData(TypeOfSample typeOfSample) throws LIMSRuntimeException;

    // public void updateData(TypeOfSample typeOfSample) throws
    // LIMSRuntimeException;

    List<TypeOfSample> getTypes(String filter, String domain) throws LIMSRuntimeException;

    List<TypeOfSample> getTypesForDomain(SampleDomain domain) throws LIMSRuntimeException;

    Integer getTotalTypeOfSampleCount() throws LIMSRuntimeException;

    TypeOfSample getTypeOfSampleByDescriptionAndDomain(TypeOfSample tos, boolean ignoreCase)
            throws LIMSRuntimeException;

    TypeOfSample getTypeOfSampleById(String typeOfSampleId) throws LIMSRuntimeException;

    List<TypeOfSample> getTypesForDomainBySortOrder(SampleDomain human) throws LIMSRuntimeException;

    TypeOfSample getTypeOfSampleByLocalAbbrevAndDomain(String localAbbrev, String domain) throws LIMSRuntimeException;

    TypeOfSample getSampleTypeFromTest(Test test);

    void clearMap();

    boolean duplicateTypeOfSampleExists(TypeOfSample typeOfSample) throws LIMSRuntimeException;

    TypeOfSample getTypeOfSampleByLocalizedName(String typeOfSampleName, Locale locale);
}
