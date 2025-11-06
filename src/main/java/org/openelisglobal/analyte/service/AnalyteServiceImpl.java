package org.openelisglobal.analyte.service;

import org.openelisglobal.analyte.dao.AnalyteDAO;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyteServiceImpl extends AuditableBaseObjectServiceImpl<Analyte, String> implements AnalyteService {
    @Autowired
    protected AnalyteDAO baseObjectDAO;

    AnalyteServiceImpl() {
        super(Analyte.class);
    }

    @Override
    protected AnalyteDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Analyte getAnalyteByName(Analyte analyte, boolean ignoreCase) {
        return getBaseObjectDAO().getAnalyteByName(analyte, ignoreCase);
    }

    @Override
    public String insert(Analyte analyte) {
        if (duplicateAnalyteExists(analyte)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + analyte.getAnalyteName());
        }
        return super.insert(analyte);
    }

    @Override
    public Analyte save(Analyte analyte) {
        if (duplicateAnalyteExists(analyte)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + analyte.getAnalyteName());
        }
        return super.save(analyte);
    }

    @Override
    public Analyte update(Analyte analyte) {
        if (duplicateAnalyteExists(analyte)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + analyte.getAnalyteName());
        }
        return super.update(analyte);
    }

    private boolean duplicateAnalyteExists(Analyte analyte) {
        return baseObjectDAO.duplicateAnalyteExists(analyte);
    }

    @Override
    public void delete(Analyte analyte) {
        Analyte oldData = get(analyte.getId());
        oldData.setIsActive(IActionConstants.NO);
        oldData.setSysUserId(analyte.getSysUserId());
        updateDelete(oldData);
    }
}
