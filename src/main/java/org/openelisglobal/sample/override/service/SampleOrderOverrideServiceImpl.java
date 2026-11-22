package org.openelisglobal.sample.override.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.sample.override.dao.SampleOrderOverrideDAO;
import org.openelisglobal.sample.override.valueholder.OverrideReasonCode;
import org.openelisglobal.sample.override.valueholder.OverrideType;
import org.openelisglobal.sample.override.valueholder.SampleOrderOverride;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SampleOrderOverrideServiceImpl extends BaseObjectServiceImpl<SampleOrderOverride, Long>
        implements SampleOrderOverrideService {

    @Autowired
    private SampleOrderOverrideDAO sampleOrderOverrideDAO;

    public SampleOrderOverrideServiceImpl() {
        super(SampleOrderOverride.class);
    }

    @Override
    protected SampleOrderOverrideDAO getBaseObjectDAO() {
        return sampleOrderOverrideDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleOrderOverride> findBySampleId(Long sampleId) {
        return sampleOrderOverrideDAO.findBySampleId(sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SampleOrderOverride> findBySampleIdAndType(Long sampleId, OverrideType overrideType) {
        return sampleOrderOverrideDAO.findBySampleIdAndType(sampleId, overrideType);
    }

    @Override
    public SampleOrderOverride record(Long sampleId, OverrideType overrideType, OverrideReasonCode reasonCode,
            String reason, Long userId) {
        SampleOrderOverride override = sampleOrderOverrideDAO.findBySampleIdAndType(sampleId, overrideType)
                .orElseGet(SampleOrderOverride::new);
        override.setSampleId(sampleId);
        override.setOverrideType(overrideType);
        override.setReasonCode(reasonCode);
        override.setReason(reason);
        override.setOverrideUserId(userId);
        override.setRecordedDate(Timestamp.from(Instant.now()));
        if (userId != null) {
            override.setSysUserId(String.valueOf(userId));
        }
        if (override.getId() == null) {
            insert(override);
            return override;
        }
        return update(override);
    }

    @Override
    public void clear(Long sampleId, OverrideType overrideType) {
        sampleOrderOverrideDAO.findBySampleIdAndType(sampleId, overrideType).ifPresent(this::delete);
    }
}
