package org.openelisglobal.barcode.daoimpl;

import org.openelisglobal.barcode.dao.SampleItemBarcodeInfoDAO;
import org.openelisglobal.barcode.valueholder.SampleItemBarcodeInfo;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class SampleItemBarcodeInfoDAOImpl extends BaseDAOImpl<SampleItemBarcodeInfo, Integer>
        implements SampleItemBarcodeInfoDAO {

    public SampleItemBarcodeInfoDAOImpl() {
        super(SampleItemBarcodeInfo.class);
    }
}
