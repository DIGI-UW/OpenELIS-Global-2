package org.openelisglobal.sample.override.daoimpl;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.sample.override.dao.SampleOrderOverrideDAO;
import org.openelisglobal.sample.override.valueholder.OverrideType;
import org.openelisglobal.sample.override.valueholder.SampleOrderOverride;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class SampleOrderOverrideDAOImpl extends BaseDAOImpl<SampleOrderOverride, Long>
        implements SampleOrderOverrideDAO {

    private static final Logger logger = LoggerFactory.getLogger(SampleOrderOverrideDAOImpl.class);

    public SampleOrderOverrideDAOImpl() {
        super(SampleOrderOverride.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleOrderOverride> findBySampleId(Long sampleId) {
        try {
            return entityManager
                    .createQuery("FROM SampleOrderOverride WHERE sampleId = :sampleId", SampleOrderOverride.class)
                    .setParameter("sampleId", sampleId).getResultList();
        } catch (Exception e) {
            logger.error("Error retrieving order overrides for sample: {}", sampleId, e);
            throw new LIMSRuntimeException("Error retrieving order overrides by sample ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SampleOrderOverride> findBySampleIdAndType(Long sampleId, OverrideType overrideType) {
        try {
            return entityManager
                    .createQuery("FROM SampleOrderOverride WHERE sampleId = :sampleId AND overrideType = :overrideType",
                            SampleOrderOverride.class)
                    .setParameter("sampleId", sampleId).setParameter("overrideType", overrideType).getResultList()
                    .stream().findFirst();
        } catch (Exception e) {
            logger.error("Error retrieving {} override for sample: {}", overrideType, sampleId, e);
            throw new LIMSRuntimeException("Error retrieving order override", e);
        }
    }
}
