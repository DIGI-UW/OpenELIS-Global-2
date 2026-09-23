package org.openelisglobal.accreditation.daoimpl;

import java.util.List;
import org.openelisglobal.accreditation.dao.AccreditingBodyDAO;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AccreditingBodyDAOImpl extends BaseDAOImpl<AccreditingBody, Long> implements AccreditingBodyDAO {

    public AccreditingBodyDAOImpl() {
        super(AccreditingBody.class);
    }

    /** Report order: display_order, ties broken alphabetically on code. */
    @Override
    @Transactional(readOnly = true)
    public List<AccreditingBody> getAllOrdered() {
        return getAllOrdered(List.of("displayOrder", "code"), false);
    }
}
