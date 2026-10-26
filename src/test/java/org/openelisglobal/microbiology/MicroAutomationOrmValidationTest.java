package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.workplan.valueholder.WorkplanPrintRecord;

public class MicroAutomationOrmValidationTest {
    @Test
    public void incomingBenchAndSharedPrintRecordsHaveValidMappings() {
        var c = new Configuration();
        for (var type : java.util.List.of(MicroIncomingItem.class, MicroCase.class, MicroBenchAction.class,
                WorkplanPrintRecord.class))
            c.addAnnotatedClass(type);
        c.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        c.setProperty("hibernate.hbm2ddl.auto", "none");
        c.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");
        try (var factory = c.buildSessionFactory()) {
            assertEquals(4, factory.getMetamodel().getEntities().size());
            assertEquals(MicroCase.class,
                    factory.getMetamodel().entity(MicroIncomingItem.class).getAttribute("microCase").getJavaType());
            assertEquals(String.class,
                    factory.getMetamodel().entity(WorkplanPrintRecord.class).getAttribute("snapshot").getJavaType());
        }
    }
}
