package org.openelisglobal.microbiology.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCultureReadingDAO;
import org.openelisglobal.microbiology.valueholder.MicroCultureReading;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCultureReadingDAOImpl extends BaseDAOImpl<MicroCultureReading, String>
        implements MicroCultureReadingDAO {
    public MicroCultureReadingDAOImpl() {
        super(MicroCultureReading.class);
    }
}
