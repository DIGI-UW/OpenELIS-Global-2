package org.openelisglobal.result.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.springframework.security.access.prepost.PreAuthorize;

@CrossDomainService(callers = "result entry, result modification and pathology sign-off - three distinct"
        + " privilege contexts")
public interface LogbookResultsPersistService {

    /**
     * Persists a set of results. Three privilege contexts reach it: the results
     * logbook (result:enter), result modification (result:modify), and pathology
     * sign-off, which calls this to finalize the analyses and release the case. All
     * three programme services do so -
     * {@code CytologySampleServiceImpl.updateWithFormValues},
     * {@code PathologySampleServiceImpl} and
     * {@code ImmunohistochemistrySampleServiceImpl}.
     *
     * <p>
     * Signing a case out IS what result:pathology-sign-off exists for, but neither
     * pathology role holds result:enter or result:modify, so the sign-off denied
     * here - after the case had been reviewed. Granting those two privileges
     * instead would have let a pathologist enter and amend ordinary bench results,
     * which is not their role.
     */
    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_ENTER','PRIV_RESULT_MODIFY','PRIV_RESULT_PATHOLOGY_SIGN_OFF')")
    List<Analysis> persistDataSet(ResultsUpdateDataSet actionDataSet, List<IResultUpdate> updaters, String sysUserId);
}
