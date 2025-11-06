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
package org.openelisglobal.inventory.daoimpl;

import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.inventory.dao.InventoryLocationDAO;
import org.openelisglobal.inventory.valueholder.InventoryLocation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class InventoryLocationDAOImpl extends BaseDAOImpl<InventoryLocation, String> implements InventoryLocationDAO {

    public InventoryLocationDAOImpl() {
        super(InventoryLocation.class);
    }

    public InventoryLocation readInventoryLocation(String idString) throws LIMSRuntimeException {
        InventoryLocation data = null;
        try {
            data = entityManager.unwrap(Session.class).get(InventoryLocation.class, idString);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in InventoryLocation readInventoryLocation()", e);
        }

        return data;
    }
}
