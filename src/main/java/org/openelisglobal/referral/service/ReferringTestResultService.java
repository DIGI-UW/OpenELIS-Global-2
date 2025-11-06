package org.openelisglobal.referral.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.referral.valueholder.ReferringTestResult;

public interface ReferringTestResultService extends BaseObjectService<ReferringTestResult, String> {

    List<ReferringTestResult> getReferringTestResultsForSampleItem(String id);
}
