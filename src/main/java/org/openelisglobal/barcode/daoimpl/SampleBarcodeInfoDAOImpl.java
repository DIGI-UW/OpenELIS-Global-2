package org.openelisglobal.barcode.daoimpl;

import org.openelisglobal.barcode.dao.SampleBarcodeInfoDAO;
import org.openelisglobal.barcode.valueholder.SampleBarcodeInfo;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class SampleBarcodeInfoDAOImpl extends BaseDAOImpl<SampleBarcodeInfo, Integer> implements SampleBarcodeInfoDAO {

    public SampleBarcodeInfoDAOImpl() {
        super(SampleBarcodeInfo.class);
    }
}
