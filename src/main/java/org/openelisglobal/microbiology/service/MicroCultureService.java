package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.*;

public interface MicroCultureService {
    List<MicroCultureForm> getRows(String caseId);

    MicroCultureOptionsForm getCatalog();

    MicroCultureOptionsForm getOptions(String caseId);

    List<MicroCultureForm> inoculate(String caseId, MicroCultureRequestForm request, String actor);

    List<MicroCultureForm> act(String caseId, String rowId, String action, MicroCultureRequestForm request,
            String actor);
}
