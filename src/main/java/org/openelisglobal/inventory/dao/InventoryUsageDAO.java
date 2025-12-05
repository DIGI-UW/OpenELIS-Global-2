package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.inventory.valueholder.InventoryUsage;

public interface InventoryUsageDAO extends BaseDAO<InventoryUsage, String> {

    List<InventoryUsage> findByLotId(String lotId);

    List<InventoryUsage> findByTestResultId(String testResultId);
}
