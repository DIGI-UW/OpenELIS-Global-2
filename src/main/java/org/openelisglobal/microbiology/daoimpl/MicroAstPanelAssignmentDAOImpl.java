package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroAstPanelAssignmentDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstPanelAssignment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroAstPanelAssignmentDAOImpl extends BaseDAOImpl<MicroAstPanelAssignment, String>
        implements MicroAstPanelAssignmentDAO {
    public MicroAstPanelAssignmentDAOImpl() {
        super(MicroAstPanelAssignment.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstPanelAssignment> getByIsolateId(String id) {
        return entityManager
                .createQuery("from MicroAstPanelAssignment e where e.isolateId = :id", MicroAstPanelAssignment.class)
                .setParameter("id", id).getResultList();
    }
}
