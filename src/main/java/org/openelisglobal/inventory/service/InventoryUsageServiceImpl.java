package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.inventory.dao.InventoryItemDAO;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.dao.InventoryUsageDAO;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryUsageServiceImpl extends AuditableBaseObjectServiceImpl<InventoryUsage, Long>
        implements InventoryUsageService {

    @Autowired
    private InventoryUsageDAO inventoryUsageDAO;

    @Autowired
    private InventoryLotDAO inventoryLotDAO;

    @Autowired
    private InventoryItemDAO inventoryItemDAO;

    public InventoryUsageServiceImpl() {
        super(InventoryUsage.class);
    }

    @Override
    protected InventoryUsageDAO getBaseObjectDAO() {
        return inventoryUsageDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> getByTestResultId(Long testResultId) {
        return inventoryUsageDAO.getByTestResultId(testResultId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> getByLotId(Long lotId) {
        return inventoryUsageDAO.getByLotId(lotId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> getByInventoryItemId(Long itemId) {
        return inventoryUsageDAO.getByInventoryItemId(itemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> getByAnalysisId(Long analysisId) {
        return inventoryUsageDAO.getByAnalysisId(analysisId);
    }

    @Override
    @Transactional
    public InventoryUsage recordUsage(Long lotId, Long itemId, Double quantityUsed, Long testResultId, Long analysisId,
            String sysUserId) {

        InventoryLot lot = inventoryLotDAO.get(lotId)
                .orElseThrow(() -> new IllegalArgumentException("Lot not found: " + lotId));

        InventoryItem item = inventoryItemDAO.get(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found: " + itemId));

        if (quantityUsed == null || quantityUsed <= 0) {
            throw new IllegalArgumentException("Quantity used must be greater than 0");
        }

        InventoryUsage usage = new InventoryUsage();
        usage.setLot(lot);
        usage.setInventoryItem(item);
        usage.setQuantityUsed(quantityUsed);
        usage.setTestResultId(testResultId);
        usage.setAnalysisId(analysisId);
        usage.setUsageDate(new Timestamp(System.currentTimeMillis()));
        usage.setSysUserId(sysUserId);
        usage.setPerformedByUser(Integer.valueOf(sysUserId));

        Long id = insert(usage);
        return get(id);
    }
}
