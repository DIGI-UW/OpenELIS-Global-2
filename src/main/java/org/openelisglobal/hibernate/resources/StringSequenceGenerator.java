package org.openelisglobal.hibernate.resources;

import java.io.Serializable;
import java.util.Properties;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;

public class StringSequenceGenerator extends SequenceStyleGenerator {
    private String numberFormat = "%d";

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        Long id = (Long) super.generate(session, object);
        return String.format(numberFormat, id);
    }

    @Override
    public void configure(Type type, Properties params, ServiceRegistry dialect) throws MappingException {
        super.configure(LongType.INSTANCE, params, dialect);
    }
}
