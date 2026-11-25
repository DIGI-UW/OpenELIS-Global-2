package org.openelisglobal.dictionaryterminology.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.dictionaryterminology.dao.DictionaryTerminologyMappingDAO;
import org.openelisglobal.dictionaryterminology.valueholder.DictionaryTerminologyMapping;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DictionaryTerminologyMappingDAOImpl extends BaseDAOImpl<DictionaryTerminologyMapping, String>
        implements DictionaryTerminologyMappingDAO {

    public DictionaryTerminologyMappingDAOImpl() {
        super(DictionaryTerminologyMapping.class);
    }
}
