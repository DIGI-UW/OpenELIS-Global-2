package org.openelisglobal.barcode.service;

import org.openelisglobal.barcode.dao.BarcodeLabelInfoDAO;
import org.openelisglobal.barcode.valueholder.BarcodeLabelInfo;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BarcodeLabelInfoServiceImpl extends AuditableBaseObjectServiceImpl<BarcodeLabelInfo, String>
        implements BarcodeLabelInfoService {
    @Autowired
    protected BarcodeLabelInfoDAO baseObjectDAO;

    BarcodeLabelInfoServiceImpl() {
        super(BarcodeLabelInfo.class);
    }

    @Override
    protected BarcodeLabelInfoDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public BarcodeLabelInfo getDataByCode(String code) {
        return getMatch("code", code).orElse(null);
    }
}
