package org.openelisglobal.microbiology.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCultureExtensionDAO;
import org.openelisglobal.microbiology.valueholder.MicroCultureExtension;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCultureExtensionDAOImpl extends BaseDAOImpl<MicroCultureExtension, String>
        implements MicroCultureExtensionDAO {
    public MicroCultureExtensionDAOImpl() {
        super(MicroCultureExtension.class);
    }
}
