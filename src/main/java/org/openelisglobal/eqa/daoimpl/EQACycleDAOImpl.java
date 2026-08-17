package org.openelisglobal.eqa.daoimpl;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQACycleDAOImpl extends BaseDAOImpl<EQACycle, Long> implements EQACycleDAO {

    public EQACycleDAOImpl() {
        super(EQACycle.class);
    }

    @Override
    public Optional<EQACycle> getForUpdate(Long id) {
        return Optional.ofNullable(entityManager.find(EQACycle.class, id, LockModeType.PESSIMISTIC_WRITE));
    }
}
