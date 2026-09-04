package org.openelisglobal.dataexchange.service.orderresult;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.dataexchange.orderresult.valueholder.HL7MessageOut;

@CrossDomainService(callers = "HL7 outbound messaging pipeline — internal infrastructure")
public interface HL7MessageOutService extends BaseObjectService<HL7MessageOut, String> {

    HL7MessageOut getByData(String msg);
}
