package org.openelisglobal.containertype.daoimpl;

import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.containertype.dao.ContainerTypeDAO;
import org.openelisglobal.containertype.valueholder.ContainerType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ContainerTypeDAOImpl extends BaseDAOImpl<ContainerType, Long> implements ContainerTypeDAO {

    public ContainerTypeDAOImpl() {
        super(ContainerType.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContainerType> findByCode(String code) {
        return entityManager.createQuery("FROM ContainerType c WHERE upper(c.code) = upper(:code)", ContainerType.class)
                .setParameter("code", code).getResultList().stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public int countSamplesUsing(String containerName) {
        Number count = (Number) entityManager
                .createNativeQuery("SELECT count(*) FROM clinlims.sample_item WHERE container = :name")
                .setParameter("name", containerName).getSingleResult();
        return count.intValue();
    }

    @Override
    public int importEnvironmentalDictionary() {
        int added = entityManager
                .createNativeQuery("INSERT INTO clinlims.container_type (id, code, name, is_active,"
                        + " edited, last_updated) SELECT nextval('clinlims.container_type_seq'), 'ENV-' || d.id,"
                        + " left(d.dict_entry, 120), d.is_active = 'Y', false, now() FROM clinlims.dictionary d"
                        + " JOIN clinlims.dictionary_category dc ON dc.id = d.dictionary_category_id"
                        + " WHERE dc.name = 'Sample Container'"
                        + " AND NOT EXISTS (SELECT 1 FROM clinlims.container_type ct WHERE ct.code = 'ENV-' || d.id)")
                .executeUpdate();
        entityManager.createNativeQuery("INSERT INTO clinlims.container_type_domain (container_type_id, domain)"
                + " SELECT ct.id, 'ENVIRONMENTAL' FROM clinlims.container_type ct WHERE ct.code LIKE 'ENV-%'"
                + " AND NOT EXISTS (SELECT 1 FROM clinlims.container_type_domain x"
                + " WHERE x.container_type_id = ct.id AND x.domain = 'ENVIRONMENTAL')").executeUpdate();
        return added;
    }
}
