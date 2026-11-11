package org.openelisglobal.typeofsample.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.typeofsample.dao.TypeOfSamplePanelDAO;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TypeOfSamplePanelServiceImpl extends AuditableBaseObjectServiceImpl<TypeOfSamplePanel, String>
        implements TypeOfSamplePanelService {
    @Autowired
    protected TypeOfSamplePanelDAO baseObjectDAO;
    @Autowired
    private PanelItemService panelItemService;
    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;

    TypeOfSamplePanelServiceImpl() {
        super(TypeOfSamplePanel.class);
    }

    @Override
    protected TypeOfSamplePanelDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(TypeOfSamplePanel typeOfSamplePanel) {
        getBaseObjectDAO().getData(typeOfSamplePanel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSamplePanel> getAllTypeOfSamplePanels() {
        return getBaseObjectDAO().getAllTypeOfSamplePanels();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSamplePanel> getPageOfTypeOfSamplePanel(int startingRecNo) {
        return getBaseObjectDAO().getPageOfTypeOfSamplePanel(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalTypeOfSamplePanelCount() {
        return getBaseObjectDAO().getTotalTypeOfSamplePanelCount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSamplePanel> getTypeOfSamplePanelsForPanel(String panelId) {
        return getBaseObjectDAO().getTypeOfSamplePanelsForPanel(panelId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSamplePanel> getTypeOfSamplePanelsForSampleType(String sampleType) {
        return getBaseObjectDAO().getTypeOfSamplePanelsForSampleType(sampleType);
    }

    @Override
    @Transactional
    public void syncPanelSampleTypes(String panelId, String sysUserId) {
        // The derived set: distinct sample types of the panel's member tests,
        // read straight from SAMPLETYPE_TEST (never the lazy display caches).
        Set<String> derived = new LinkedHashSet<>();
        for (PanelItem item : panelItemService.getPanelItemsForPanel(panelId)) {
            if (item.getTest() == null) {
                continue;
            }
            for (TypeOfSampleTest link : typeOfSampleTestService.getTypeOfSampleTestsForTest(item.getTest().getId())) {
                derived.add(link.getTypeOfSampleId());
            }
        }
        // Reconcile: insert missing links, delete stale ones. Rows for a still-
        // derived type are left untouched.
        for (TypeOfSamplePanel existing : getBaseObjectDAO().getTypeOfSamplePanelsForPanel(panelId)) {
            if (!derived.remove(existing.getTypeOfSampleId())) {
                delete(existing);
            }
        }
        for (String sampleTypeId : derived) {
            TypeOfSamplePanel link = new TypeOfSamplePanel();
            link.setPanelId(panelId);
            link.setTypeOfSampleId(sampleTypeId);
            link.setSysUserId(sysUserId);
            insert(link);
        }
    }
}
