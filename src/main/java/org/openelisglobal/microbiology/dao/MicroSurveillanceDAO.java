package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;

public interface MicroSurveillanceDAO extends BaseDAO<MicroCase, String> {
    List<Object[]> contexts(List<String> caseIds);

    List<Object[]> originalSpecimens(List<String> caseIds);

    List<String> externalSampleIds(List<String> caseIds);

    List<Object[]> outcomes(List<String> caseIds);
}
