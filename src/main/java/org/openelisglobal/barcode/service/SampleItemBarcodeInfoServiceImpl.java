package org.openelisglobal.barcode.service;

import org.openelisglobal.barcode.dao.SampleItemBarcodeInfoDAO;
import org.openelisglobal.barcode.valueholder.SampleItemBarcodeInfo;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SampleItemBarcodeInfoServiceImpl extends AuditableBaseObjectServiceImpl<SampleItemBarcodeInfo, Integer>
        implements SampleItemBarcodeInfoService {

    @Autowired
    protected SampleItemBarcodeInfoDAO baseObjectDAO;

    public SampleItemBarcodeInfoServiceImpl() {
        super(SampleItemBarcodeInfo.class);
    }

    @Override
    protected SampleItemBarcodeInfoDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
