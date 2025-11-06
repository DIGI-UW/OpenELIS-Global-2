package org.openelisglobal.testtrailer.service;

import java.util.List;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testtrailer.dao.TestTrailerDAO;
import org.openelisglobal.testtrailer.valueholder.TestTrailer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestTrailerServiceImpl extends AuditableBaseObjectServiceImpl<TestTrailer, String>
        implements TestTrailerService {
    @Autowired
    protected TestTrailerDAO baseObjectDAO;

    TestTrailerServiceImpl() {
        super(TestTrailer.class);
    }

    @Override
    protected TestTrailerDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(TestTrailer testTrailer) {
        getBaseObjectDAO().getData(testTrailer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestTrailer> getPageOfTestTrailers(int startingRecNo) {
        return getBaseObjectDAO().getPageOfTestTrailers(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalTestTrailerCount() {
        return getBaseObjectDAO().getTotalTestTrailerCount();
    }

    @Override
    @Transactional(readOnly = true)
    public TestTrailer getTestTrailerByName(TestTrailer testTrailer) {
        return getBaseObjectDAO().getTestTrailerByName(testTrailer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestTrailer> getAllTestTrailers() {
        return getBaseObjectDAO().getAllTestTrailers();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestTrailer> getTestTrailers(String filter) {
        return getBaseObjectDAO().getTestTrailers(filter);
    }

    @Override
    public String insert(TestTrailer testTrailer) {
        if (duplicateTestTrailerExists(testTrailer)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + testTrailer.getTestTrailerName());
        }
        return super.insert(testTrailer);
    }

    @Override
    public TestTrailer save(TestTrailer testTrailer) {
        if (duplicateTestTrailerExists(testTrailer)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + testTrailer.getTestTrailerName());
        }
        return super.save(testTrailer);
    }

    @Override
    public TestTrailer update(TestTrailer testTrailer) {
        if (duplicateTestTrailerExists(testTrailer)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + testTrailer.getTestTrailerName());
        }
        return super.update(testTrailer);
    }

    private boolean duplicateTestTrailerExists(TestTrailer testTrailer) {
        return baseObjectDAO.duplicateTestTrailerExists(testTrailer);
    }
}
