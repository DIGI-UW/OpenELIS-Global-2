package org.openelisglobal.result.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.springframework.security.access.prepost.PreAuthorize;

@CrossDomainService(callers = "result entry and result modification — two distinct privilege contexts")
public interface LogbookResultsPersistService {

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_ENTER','PRIV_RESULT_MODIFY')")
    List<Analysis> persistDataSet(ResultsUpdateDataSet actionDataSet, List<IResultUpdate> updaters, String sysUserId);
}
