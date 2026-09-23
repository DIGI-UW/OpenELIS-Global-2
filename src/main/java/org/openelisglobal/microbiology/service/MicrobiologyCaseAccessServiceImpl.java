package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicrobiologyCaseAccessServiceImpl implements MicrobiologyCaseAccessService {

    private final MicroCaseService caseService;
    private final SampleItemService sampleItemService;
    private final AnalysisService analysisService;
    private final UserService userService;

    public MicrobiologyCaseAccessServiceImpl(MicroCaseService caseService, SampleItemService sampleItemService,
            AnalysisService analysisService, UserService userService) {
        this.caseService = caseService;
        this.sampleItemService = sampleItemService;
        this.analysisService = analysisService;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAccessCase(String caseId, String systemUserId, boolean administrator) {
        if (administrator) {
            return true;
        }
        MicroCase microCase = caseService.getCase(caseId);
        return microCase != null && canAccessSampleItem(microCase.getSampleItemId(), systemUserId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAccessSampleItem(String sampleItemId, String systemUserId, boolean administrator) {
        if (administrator) {
            return true;
        }
        if (systemUserId == null || sampleItemId == null) {
            return false;
        }
        SampleItem sampleItem = sampleItemService.get(sampleItemId);
        if (sampleItem == null) {
            return false;
        }
        List<Analysis> analyses = analysisService.getAnalysesBySampleItem(sampleItem);
        if (analyses == null || analyses.isEmpty()) {
            return false;
        }
        return hasAuthorizedAnalysis(systemUserId, analyses, Constants.ROLE_RESULTS)
                || hasAuthorizedAnalysis(systemUserId, analyses, Constants.ROLE_VALIDATION);
    }

    private boolean hasAuthorizedAnalysis(String systemUserId, List<Analysis> analyses, String role) {
        List<Analysis> authorized = userService.filterAnalysesByLabUnitRoles(systemUserId, analyses, role);
        return authorized != null && !authorized.isEmpty();
    }
}
