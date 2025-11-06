package org.openelisglobal.datasubmission.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.datasubmission.dao.TypeOfDataIndicatorDAO;
import org.openelisglobal.datasubmission.valueholder.TypeOfDataIndicator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TypeOfDataIndicatorDAOImpl extends BaseDAOImpl<TypeOfDataIndicator, String>
        implements TypeOfDataIndicatorDAO {

    public TypeOfDataIndicatorDAOImpl() {
        super(TypeOfDataIndicator.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(TypeOfDataIndicator typeOfIndicator) throws LIMSRuntimeException {
        try {
            TypeOfDataIndicator typeOfIndicatorClone = entityManager.unwrap(Session.class)
                    .get(TypeOfDataIndicator.class, typeOfIndicator.getId());
            if (typeOfIndicatorClone != null) {
                PropertyUtils.copyProperties(typeOfIndicator, typeOfIndicatorClone);
            } else {
                typeOfIndicator.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TypeOfDataIndicator getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfDataIndicator> getAllTypeOfDataIndicator() throws LIMSRuntimeException {
        List<TypeOfDataIndicator> list;
        try {
            String sql = "from TypeOfDataIndicator";
            Query<TypeOfDataIndicator> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TypeOfDataIndicator.class);
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TypeOfDataIndicator getAllTypeOfDataIndicator()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public TypeOfDataIndicator getTypeOfDataIndicator(String id) throws LIMSRuntimeException {
        try {
            TypeOfDataIndicator dataValue = entityManager.unwrap(Session.class).get(TypeOfDataIndicator.class, id);
            return dataValue;
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TypeOfDataIndicator getData()", e);
        }
    }
}
