package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.form.MicroWorklistActivityContext;
import org.openelisglobal.microbiology.form.MicroWorklistRecentActivityContext;
import org.openelisglobal.microbiology.form.MicroWorklistSpecimenContext;
import org.openelisglobal.microbiology.valueholder.MicroCase;

public interface MicroWorklistContextDAO extends BaseDAO<MicroCase, String> {

    List<MicroWorklistSpecimenContext> getSpecimenContexts(List<String> sampleItemIds);

    List<MicroWorklistActivityContext> getLatestActivityContexts(List<String> caseIds);

    List<MicroWorklistRecentActivityContext> getRecentActivityContexts(List<String> caseIds, int limit);
}
