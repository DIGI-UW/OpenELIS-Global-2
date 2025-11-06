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
package org.openelisglobal.citystatezip.dao;

import java.util.List;
import org.openelisglobal.citystatezip.valueholder.CityStateZip;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface CityStateZipDAO extends BaseDAO<CityStateZip, String> {

    List<CityStateZip> getCities(String filter) throws LIMSRuntimeException;

    List<CityStateZip> getZipCodesByCity(CityStateZip cityStateZip) throws LIMSRuntimeException;

    List<CityStateZip> getCitiesByZipCode(CityStateZip cityStateZip) throws LIMSRuntimeException;

    List<CityStateZip> getAllStateCodes() throws LIMSRuntimeException;

    CityStateZip getCityStateZipByCityAndZipCode(CityStateZip cityStateZip) throws LIMSRuntimeException;

    CityStateZip getState(CityStateZip cityStateZip) throws LIMSRuntimeException;

    // bugzilla 1765
    CityStateZip getCity(CityStateZip cityStateZip) throws LIMSRuntimeException;

    // bugzilla 1765
    CityStateZip getZipCode(CityStateZip cityStateZip) throws LIMSRuntimeException;

    // bugzilla 1765
    boolean isCityStateZipComboValid(CityStateZip cityStateZip) throws LIMSRuntimeException;

    // bugzilla 1765
    List<CityStateZip> getValidCityStateZipCombosForHumanSampleEntry(CityStateZip cityStateZip)
            throws LIMSRuntimeException;

    // bugizla 2393
    String getCountyCodeByStateAndZipCode(CityStateZip cityStateZip) throws LIMSRuntimeException;
}
