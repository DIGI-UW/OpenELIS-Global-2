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
package org.openelisglobal.provider.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.valueholder.Provider;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface ProviderDAO extends BaseDAO<Provider, String> {

    // public boolean insertData(Provider provider) throws LIMSRuntimeException;

    // public void deleteData(List providers) throws LIMSRuntimeException;

    List<Provider> getAllProviders() throws LIMSRuntimeException;

    List<Provider> getPageOfProviders(int startingRecNo) throws LIMSRuntimeException;

    void getData(Provider provider) throws LIMSRuntimeException;

    // public void updateData(Provider provider) throws LIMSRuntimeException;

    /*
     * The intent of this is to find the provider linked to a person. It assumes
     * that each provider is uniquely linked to a person. If more than one provider
     * is linked to the same person then the first provider is returned.
     */
    Provider getProviderByPerson(Person person) throws LIMSRuntimeException;

    int getTotalSearchedProviderCount(String parameter);

    List<Provider> getPagesOfSearchedProviders(int startingRecNo, String parameter);
}
