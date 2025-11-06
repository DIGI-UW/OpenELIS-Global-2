package org.openelisglobal.dataexchange.service.orderresult;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.dataexchange.orderresult.dao.HL7MessageOutDAO;
import org.openelisglobal.dataexchange.orderresult.valueholder.HL7MessageOut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HL7MessageOutServiceImpl extends AuditableBaseObjectServiceImpl<HL7MessageOut, String>
        implements HL7MessageOutService {
    @Autowired
    protected HL7MessageOutDAO baseObjectDAO;

    HL7MessageOutServiceImpl() {
        super(HL7MessageOut.class);
    }

    @Override
    protected HL7MessageOutDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public HL7MessageOut getByData(String msg) {
        // TODO Auto-generated method stub
        return null;
    }
}
