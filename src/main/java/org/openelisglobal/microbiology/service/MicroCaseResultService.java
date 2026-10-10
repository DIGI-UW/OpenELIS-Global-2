package org.openelisglobal.microbiology.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.microbiology.form.*;

public interface MicroCaseResultService {
    List<MicroCaseTestForm> getTests(String caseId, String userId);

    List<MicroCaseTestForm> addTests(String caseId, MicroCaseAddTestsForm request, String userId);

    List<MicroCaseTestForm> saveResults(String caseId, String analysisId, MicroCaseResultRequestForm request,
            String userId, HttpServletRequest httpRequest);

    List<MicroCaseTestForm> setTestedElsewhere(String caseId, String analysisId,
            MicroCaseTestedElsewhereRequestForm request, String userId);

    List<MicroCaseTestForm> validateResult(String caseId, String analysisId, String version, String userId);
}
