package org.openelisglobal.sampletypeterminology.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.sampletypeterminology.dao.SampleTypeTerminologyMappingDAO;
import org.openelisglobal.sampletypeterminology.valueholder.SampleTypeTerminologyMapping;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class SampleTypeTerminologyMappingDAOImpl extends BaseDAOImpl<SampleTypeTerminologyMapping, String>
        implements SampleTypeTerminologyMappingDAO {

    public SampleTypeTerminologyMappingDAOImpl() {
        super(SampleTypeTerminologyMapping.class);
    }
}
