package org.openelisglobal.vector.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.vector.dao.VectorSpeciesDAO;
import org.openelisglobal.vector.valueholder.VectorSpecies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VectorSpeciesServiceImpl extends AuditableBaseObjectServiceImpl<VectorSpecies, String>
        implements VectorSpeciesService {

    @Autowired
    protected VectorSpeciesDAO baseObjectDAO;

    public VectorSpeciesServiceImpl() {
        super(VectorSpecies.class);
    }

    @Override
    protected VectorSpeciesDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorSpecies> getByGroupId(String groupId) {
        return getBaseObjectDAO().getByGroupId(groupId);
    }
}
