package org.openelisglobal.statusofsample.service;

import java.util.List;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.statusofsample.dao.StatusOfSampleDAO;
import org.openelisglobal.statusofsample.valueholder.StatusOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatusOfSampleServiceImpl extends AuditableBaseObjectServiceImpl<StatusOfSample, String>
        implements StatusOfSampleService {
    @Autowired
    protected StatusOfSampleDAO baseObjectDAO;

    StatusOfSampleServiceImpl() {
        super(StatusOfSample.class);
    }

    @Override
    protected StatusOfSampleDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(StatusOfSample sourceOfSample) {
        getBaseObjectDAO().getData(sourceOfSample);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusOfSample> getPageOfStatusOfSamples(int startingRecNo) {
        return getBaseObjectDAO().getPageOfStatusOfSamples(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalStatusOfSampleCount() {
        return getBaseObjectDAO().getTotalStatusOfSampleCount();
    }

    @Override
    @Transactional(readOnly = true)
    public StatusOfSample getDataByStatusTypeAndStatusCode(StatusOfSample statusofsample) {
        return getBaseObjectDAO().getDataByStatusTypeAndStatusCode(statusofsample);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusOfSample> getAllStatusOfSamples() {
        return getBaseObjectDAO().getAllStatusOfSamples();
    }

    @Override
    public String insert(StatusOfSample statusOfSample) {
        if (duplicateStatusOfSampleExists(statusOfSample)) {
            StringBuffer sb = new StringBuffer();
            sb.append("Duplicate record exists for Description: ");
            sb.append(statusOfSample.getDescription());
            sb.append(" Status Type: ");
            sb.append(statusOfSample.getStatusType());
            // bugzilla 2154
            LogEvent.logError(this.getClass().getSimpleName(), "insert", sb.toString());
            throw new LIMSDuplicateRecordException(sb.toString());
        }
        return super.insert(statusOfSample);
    }

    @Override
    public StatusOfSample save(StatusOfSample statusOfSample) {
        if (duplicateStatusOfSampleExists(statusOfSample)) {
            StringBuffer sb = new StringBuffer();
            sb.append("Duplicate record exists for Description: ");
            sb.append(statusOfSample.getDescription());
            sb.append(" Status Type: ");
            sb.append(statusOfSample.getStatusType());
            // bugzilla 2154
            LogEvent.logError(this.getClass().getSimpleName(), "save", sb.toString());
            throw new LIMSDuplicateRecordException(sb.toString());
        }
        return super.save(statusOfSample);
    }

    @Override
    public StatusOfSample update(StatusOfSample statusOfSample) {
        if (duplicateStatusOfSampleExists(statusOfSample)) {
            StringBuffer sb = new StringBuffer();
            sb.append("Duplicate record exists for Description: ");
            sb.append(statusOfSample.getDescription());
            sb.append(" Status Type: ");
            sb.append(statusOfSample.getStatusType());
            // bugzilla 2154
            LogEvent.logError(this.getClass().getSimpleName(), "update", sb.toString());
            throw new LIMSDuplicateRecordException(sb.toString());
        }
        return super.update(statusOfSample);
    }

    private boolean duplicateStatusOfSampleExists(StatusOfSample statusOfSample) {
        return baseObjectDAO.duplicateStatusOfSampleExists(statusOfSample);
    }
}
