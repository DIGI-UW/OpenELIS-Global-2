package org.openelisglobal.program.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.program.valueholder.pathology.PathologyConsultation;

/**
 * Reads and writes the second opinions of a pathology case, which the case
 * itself does not hold.
 */
public interface PathologyConsultationDAO extends BaseDAO<PathologyConsultation, Integer> {

    /** Every second opinion on the case, newest first. */
    List<PathologyConsultation> getForCase(Integer pathologySampleId);
}
