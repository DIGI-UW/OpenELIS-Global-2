package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationEvent;

public interface MicroIsolateIdentificationEventDAO extends BaseDAO<MicroIsolateIdentificationEvent, String> {

    List<MicroIsolateIdentificationEvent> getByIsolateId(String isolateId);

    List<MicroIsolateIdentificationEvent> getByAmendmentId(String amendmentId);
}
