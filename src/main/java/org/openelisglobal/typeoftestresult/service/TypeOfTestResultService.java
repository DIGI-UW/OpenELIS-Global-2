package org.openelisglobal.typeoftestresult.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.openelisglobal.typeoftestresult.valueholder.TypeOfTestResult;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
public interface TypeOfTestResultService extends BaseObjectService<TypeOfTestResult, String> {

    TypeOfTestResult getTypeOfTestResultByType(TypeOfTestResult typeOfTestResult);

    TypeOfTestResult getTypeOfTestResultByType(String type);

    ResultType getResultTypeById(String resultTypeId);
}
