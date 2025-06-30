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
package org.openelisglobal.inventory.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.inventory.dao.InventoryReceiptDAO;
import org.openelisglobal.inventory.valueholder.InventoryReceipt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class InventoryReceiptDAOImpl extends BaseDAOImpl<InventoryReceipt, String> implements InventoryReceiptDAO {

    public InventoryReceiptDAOImpl() {
        super(InventoryReceipt.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryReceipt> getAllInventoryReceipts() throws LIMSRuntimeException {
        List<InventoryReceipt> inventoryReceipts;
        try {
            String sql = "from InventoryReceipt";
            Query<InventoryReceipt> query = entityManager.unwrap(Session.class).createQuery(sql,
                    InventoryReceipt.class);
            inventoryReceipts = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in InventoryReceipt getAllInventoryReceipts()", e);
        }

        return inventoryReceipts;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(InventoryReceipt inventoryReceipt) throws LIMSRuntimeException {
        try {
            InventoryReceipt tmpInventoryReceipt = entityManager.unwrap(Session.class).get(InventoryReceipt.class,
                    inventoryReceipt.getId());
            if (tmpInventoryReceipt != null) {
                PropertyUtils.copyProperties(inventoryReceipt, tmpInventoryReceipt);
            } else {
                inventoryReceipt.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in InventoryReceipt getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryReceipt getInventoryReceiptById(String idString) throws LIMSRuntimeException {
        InventoryReceipt data = null;
        try {
            data = entityManager.unwrap(Session.class).get(InventoryReceipt.class, idString);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in InventoryReceipt readInventoryReceipt()", e);
        }

        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryReceipt getInventoryReceiptByInventoryItemId(String id) throws LIMSRuntimeException {
        InventoryReceipt inventory = null;

        try {
            List<InventoryReceipt> inventoryReceipts;

            String sql = "SELECT * FROM clinlims.inventory_receipt WHERE invitem_id = :id";
            Query query = entityManager.unwrap(Session.class).createNativeQuery(sql, InventoryReceipt.class);
            query.setParameter("id", Integer.parseInt(id));

            inventoryReceipts = query.list();

            if (inventoryReceipts != null && inventoryReceipts.size() > 0) {
                inventory = inventoryReceipts.get(0);
            }

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in InventoryReceipt getInventoryReceiptByInventoryItemId()", e);
        }

        return inventory;
    }
}
