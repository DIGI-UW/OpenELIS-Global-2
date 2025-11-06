package org.openelisglobal.result.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.result.dao.ResultSignatureDAO;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultSignatureServiceImpl extends AuditableBaseObjectServiceImpl<ResultSignature, String>
        implements ResultSignatureService {
    @Autowired
    protected ResultSignatureDAO baseObjectDAO;

    ResultSignatureServiceImpl() {
        super(ResultSignature.class);
    }

    @Override
    protected ResultSignatureDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(ResultSignature resultSignature) {
        getBaseObjectDAO().getData(resultSignature);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultSignature> getResultSignaturesByResult(Result result) {
        return getBaseObjectDAO().getResultSignaturesByResult(result);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultSignature getResultSignatureById(ResultSignature resultSignature) {
        return getBaseObjectDAO().getResultSignatureById(resultSignature);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultSignature> getResultSignaturesByResults(List<Result> resultList) {
        return getBaseObjectDAO().getResultSignaturesByResults(resultList);
    }
}
