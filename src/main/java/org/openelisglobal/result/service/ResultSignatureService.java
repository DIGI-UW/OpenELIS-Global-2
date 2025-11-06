package org.openelisglobal.result.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultSignature;

public interface ResultSignatureService extends BaseObjectService<ResultSignature, String> {
    void getData(ResultSignature resultSignature);

    List<ResultSignature> getResultSignaturesByResult(Result result);

    ResultSignature getResultSignatureById(ResultSignature resultSignature);

    List<ResultSignature> getResultSignaturesByResults(List<Result> resultList);
}
