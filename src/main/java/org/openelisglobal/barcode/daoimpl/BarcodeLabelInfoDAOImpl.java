package org.openelisglobal.barcode.daoimpl;

import org.openelisglobal.barcode.dao.BarcodeLabelInfoDAO;
import org.openelisglobal.barcode.valueholder.BarcodeLabelInfo;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Class for inserting, updating, and retrieving
 * lims/barcode/valueholder/BarcodeLabelInfo from the database
 * (clinlims.barcode_label_info)
 *
 * @author Caleb
 */
@Component
@Transactional
public class BarcodeLabelInfoDAOImpl extends BaseDAOImpl<BarcodeLabelInfo, String> implements BarcodeLabelInfoDAO {

    public BarcodeLabelInfoDAOImpl() {
        super(BarcodeLabelInfo.class);
    }
}
