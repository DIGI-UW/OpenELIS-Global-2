package org.openelisglobal.program.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample.ImmunohistochemistryStatus;

public interface ImmunohistochemistrySampleDAO extends BaseDAO<ImmunohistochemistrySample, Integer> {

    List<ImmunohistochemistrySample> getWithStatus(List<ImmunohistochemistryStatus> statuses);

    Long getCountWithStatus(List<ImmunohistochemistryStatus> statuses);

    Long getCountWithStatusBetweenDates(List<ImmunohistochemistryStatus> statuses, Timestamp from, Timestamp to);

    List<ImmunohistochemistrySample> searchWithStatusAndAccesionNumber(List<ImmunohistochemistryStatus> statuses,
            String labNumber);

    ImmunohistochemistrySample getByPathologySampleId(Integer pathologySampleId);
}
