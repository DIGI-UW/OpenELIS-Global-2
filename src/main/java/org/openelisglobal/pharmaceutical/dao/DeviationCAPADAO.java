package org.openelisglobal.pharmaceutical.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.pharmaceutical.valueholder.DeviationCAPA;

public interface DeviationCAPADAO extends BaseDAO<DeviationCAPA, Integer> {

    List<DeviationCAPA> findByAssayRunId(Integer assayRunId);

    List<DeviationCAPA> findByStatus(DeviationCAPA.CAPAStatus status);

    List<DeviationCAPA> findOpenCAPAs();

    DeviationCAPA findByDeviationNumber(String deviationNumber);
}
