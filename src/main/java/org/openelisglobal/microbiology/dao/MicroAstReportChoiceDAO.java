package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstReportChoice;

public interface MicroAstReportChoiceDAO extends BaseDAO<MicroAstReportChoice, String> {
    List<MicroAstReportChoice> getByIsolateId(String id);
}
