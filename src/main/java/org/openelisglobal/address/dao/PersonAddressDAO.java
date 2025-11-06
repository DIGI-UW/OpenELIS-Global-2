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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.address.dao;

import java.util.List;
import org.openelisglobal.address.valueholder.AddressPK;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;

public interface PersonAddressDAO extends BaseDAO<PersonAddress, AddressPK> {

    List<PersonAddress> getAddressPartsByPersonId(String personId) throws LIMSRuntimeException;

    PersonAddress getByPersonIdAndPartId(String personId, String addressPartId) throws LIMSRuntimeException;
}
