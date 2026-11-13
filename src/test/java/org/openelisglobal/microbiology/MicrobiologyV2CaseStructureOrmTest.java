package org.openelisglobal.microbiology;

import static org.junit.Assert.assertNotNull;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.microbiology.valueholder.*;

public class MicrobiologyV2CaseStructureOrmTest {
    @Test(timeout = 5000)
    public void caseOwnershipMappingsBuildWithoutDatabaseAccess() {
        Configuration config = new Configuration();
        for (Class<?> entity : new Class<?>[] { MicroCase.class, MicroCaseAnalysis.class, MicroCaseSample.class,
                MicroCaseRequest.class, MicroCaseSplit.class })
            config.addAnnotatedClass(entity);
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        config.setProperty("hibernate.hbm2ddl.auto", "none");
        config.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");
        StandardServiceRegistryBuilder registry = new StandardServiceRegistryBuilder()
                .applySettings(config.getProperties());
        try (SessionFactory factory = config.buildSessionFactory(registry.build())) {
            for (Class<?> entity : new Class<?>[] { MicroCase.class, MicroCaseAnalysis.class, MicroCaseSample.class,
                    MicroCaseRequest.class, MicroCaseSplit.class })
                assertNotNull(factory.getMetamodel().entity(entity));
        }
    }
}
