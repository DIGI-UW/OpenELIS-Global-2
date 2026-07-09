package org.openelisglobal.eqa.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.eqa.dao.SampleEQADAO;
import org.openelisglobal.eqa.valueholder.SampleEQA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SampleEQAServiceImpl extends BaseObjectServiceImpl<SampleEQA, Long> implements SampleEQAService {

    @Autowired
    private SampleEQADAO sampleEQADAO;

    public SampleEQAServiceImpl() {
        super(SampleEQA.class);
    }

    @Override
    protected SampleEQADAO getBaseObjectDAO() {
        return sampleEQADAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SampleEQA> findBySampleId(Long sampleId) {
        return sampleEQADAO.findBySampleId(sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleEQA> findByDeadlineBefore(Timestamp deadline) {
        return sampleEQADAO.findByDeadlineBefore(deadline);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleEQA> findByProgramId(Long programId) {
        return sampleEQADAO.findByProgramId(programId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleEQA> findEqaSamples() {
        return sampleEQADAO.findByIsEqaSample(true);
    }
}
