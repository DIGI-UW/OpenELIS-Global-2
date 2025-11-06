package org.openelisglobal.panelitem.service;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.dao.PanelItemDAO;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PanelItemServiceImpl extends AuditableBaseObjectServiceImpl<PanelItem, String>
        implements PanelItemService {
    @Autowired
    protected PanelItemDAO baseObjectDAO;
    @Autowired
    private PanelService panelService;

    PanelItemServiceImpl() {
        super(PanelItem.class);
    }

    @Override
    protected PanelItemDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelItem> getPanelItemsForPanel(String panelId) {
        return baseObjectDAO.getPanelItemsForPanel(panelId);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(PanelItem panelItem) {
        getBaseObjectDAO().getData(panelItem);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalPanelItemCount() {
        return getBaseObjectDAO().getTotalPanelItemCount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelItem> getPanelItemsForPanelAndItemList(String panelId, List<Integer> testList) {
        return getBaseObjectDAO().getPanelItemsForPanelAndItemList(panelId, testList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelItem> getPageOfPanelItems(int startingRecNo) {
        return getBaseObjectDAO().getPageOfPanelItems(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getDuplicateSortOrderForPanel(PanelItem panelItem) {
        return getBaseObjectDAO().getDuplicateSortOrderForPanel(panelItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelItem> getPanelItemByTestId(String id) {
        return getBaseObjectDAO().getPanelItemByTestId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelItem> getAllPanelItems() {
        return getBaseObjectDAO().getAllPanelItems();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PanelItem> getPanelItems(String filter) {
        return getBaseObjectDAO().getPanelItems(filter);
    }

    @Override
    public String insert(PanelItem panelItem) {
        if (duplicatePanelItemExists(panelItem)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + panelItem.getPanelName());
        }
        return super.insert(panelItem);
    }

    @Override
    public PanelItem save(PanelItem panelItem) {
        if (duplicatePanelItemExists(panelItem)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + panelItem.getPanelName());
        }
        return super.save(panelItem);
    }

    @Override
    public PanelItem update(PanelItem panelItem) {
        if (duplicatePanelItemExists(panelItem)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + panelItem.getPanelName());
        }
        return super.update(panelItem);
    }

    public PanelItem readPanelItem(String idString) {
        PanelItem pi;
        try {
            pi = get(idString);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in PanelItem readPanelItem()", e);
        }

        return pi;
    }

    @Override
    @Transactional
    public void updatePanelItems(List<PanelItem> panelItems, Panel panel, boolean updatePanel, String currentUser,
            List<Test> newTests) {

        for (PanelItem oldPanelItem : panelItems) {
            oldPanelItem.setSysUserId(currentUser);
        }
        deleteAll(panelItems);

        List<PanelItem> newPanelItems = new ArrayList<>();
        for (Test test : newTests) {
            PanelItem panelItem = new PanelItem();
            panelItem.setPanel(panel);
            panelItem.setTest(test);
            panelItem.setLastupdatedFields();
            panelItem.setSysUserId(currentUser);
            insert(panelItem);

            newPanelItems.add(panelItem);
        }

        if (newPanelItems.size() > 0) {
            panel.setIsActive("Y");
            panel.setSysUserId(currentUser);
            panelService.update(panel);
        } else {
            panel.setIsActive("N");
            panel.setSysUserId(currentUser);
            panelService.update(panel);
        }
    }

    @Override
    public boolean duplicatePanelItemExists(PanelItem panelItem) throws LIMSRuntimeException {
        if (panelItem.getPanel() == null && GenericValidator.isBlankOrNull(panelItem.getPanelName())) {
            throw new IllegalStateException("must be able to identify the panel to check if this is a duplicate");
        }
        List<PanelItem> existingPanelItems;
        if (panelItem.getPanel() != null && !GenericValidator.isBlankOrNull(panelItem.getPanel().getId())) {
            existingPanelItems = getPanelItemsForPanel(panelItem.getPanel().getId());
        } else {
            existingPanelItems = getPanelItemsForPanel(panelService.getIdForPanelName(panelItem.getPanelName()));
        }
        for (PanelItem existingPanelItem : existingPanelItems) {
            if ((panelItem.getTest().getId().equals(existingPanelItem.getTest().getId()))
                    || (panelItem.getTest().getDescription().equals(existingPanelItem.getTest().getDescription()))) {
                return !existingPanelItem.getId().equals(panelItem.getId());
            }
        }
        return false;
    }
}
