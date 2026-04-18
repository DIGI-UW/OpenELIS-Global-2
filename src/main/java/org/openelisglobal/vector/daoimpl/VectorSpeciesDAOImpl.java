package org.openelisglobal.vector.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.vector.dao.VectorSpeciesDAO;
import org.openelisglobal.vector.valueholder.VectorSpecies;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class VectorSpeciesDAOImpl extends BaseDAOImpl<VectorSpecies, String> implements VectorSpeciesDAO {

    public VectorSpeciesDAOImpl() {
        super(VectorSpecies.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorSpecies> getByGroupId(String groupId) throws LIMSRuntimeException {
        try {
            TypedQuery<VectorSpecies> query = entityManager.createQuery(
                    "from VectorSpecies where group.id = :groupId and active = true order by genus, species",
                    VectorSpecies.class);
            query.setParameter("groupId", groupId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in VectorSpeciesDAOImpl.getByGroupId()", e);
        }
    }
}
