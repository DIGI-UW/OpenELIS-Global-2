package org.openelisglobal.sample.daoimpl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.sample.dao.SampleAdditionalFieldDAO;
import org.openelisglobal.sample.valueholder.SampleAdditionalField;
import org.openelisglobal.sample.valueholder.SampleAdditionalField.AdditionalFieldName;
import org.openelisglobal.sample.valueholder.SampleAdditionalField.SampleAdditionalFieldId;
import org.springframework.stereotype.Component;

@Component
public class SampleAdditionalFieldDAOImpl extends BaseDAOImpl<SampleAdditionalField, SampleAdditionalFieldId>
        implements SampleAdditionalFieldDAO {

    public SampleAdditionalFieldDAOImpl() {
        super(SampleAdditionalField.class);
    }

    @Override
    public List<SampleAdditionalField> getAllForSample(String sampleId) {
        String sql = "from SampleAdditionalField s where s.sample.id = :sampleId";
        Query<SampleAdditionalField> query = entityManager.unwrap(Session.class).createQuery(sql,
                SampleAdditionalField.class);
        query.setParameter("sampleId", Integer.parseInt(sampleId));
        List<SampleAdditionalField> list = query.list();
        return list;
    }

    @Override
    public Optional<SampleAdditionalField> getFieldForSample(AdditionalFieldName fieldName, String sampleId) {
        String sql = "from SampleAdditionalField s where s.sample.id = :sampleId AND s.id.fieldName =" + " :fieldName";
        Query<SampleAdditionalField> query = entityManager.unwrap(Session.class).createQuery(sql,
                SampleAdditionalField.class);
        query.setParameter("sampleId", Integer.parseInt(sampleId));
        query.setParameter("fieldName", fieldName.name());
        SampleAdditionalField field = query.uniqueResult();
        if (field == null) {
            return Optional.empty();
        } else {
            return Optional.of(field);
        }
    }
}
