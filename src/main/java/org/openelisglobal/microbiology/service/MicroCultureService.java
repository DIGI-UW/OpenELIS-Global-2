package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.*;

public interface MicroCultureService {
    List<MicroCultureForm> benchInoculate(String caseId, MicroCultureRequestForm request, String actor,
            org.openelisglobal.microbiology.valueholder.MicroCaseActivity event);

    List<MicroCultureForm> benchAct(String caseId, String rowId, String action, MicroCultureRequestForm request,
            String actor, org.openelisglobal.microbiology.valueholder.MicroCaseActivity event);

    List<MicroCultureForm> getRows(String caseId);

    MicroCultureOptionsForm getCatalog();

    MicroCultureOptionsForm getOptions(String caseId);

    List<MicroCultureForm> inoculate(String caseId, MicroCultureRequestForm request, String actor);

    List<MicroCultureForm> act(String caseId, String rowId, String action, MicroCultureRequestForm request,
            String actor);
}
