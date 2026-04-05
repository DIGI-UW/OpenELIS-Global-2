package org.openelisglobal.barcode.service;

import org.openelisglobal.barcode.dao.SampleBarcodeInfoDAO;
import org.openelisglobal.barcode.valueholder.SampleBarcodeInfo;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SampleBarcodeInfoServiceImpl extends AuditableBaseObjectServiceImpl<SampleBarcodeInfo, Integer>
        implements SampleBarcodeInfoService {

    @Autowired
    protected SampleBarcodeInfoDAO baseObjectDAO;

    public SampleBarcodeInfoServiceImpl() {
        super(SampleBarcodeInfo.class);
    }

    @Override
    protected SampleBarcodeInfoDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
