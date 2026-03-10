package org.openelisglobal.barcode;

import static org.junit.Assert.assertNotNull;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.barcode.valueholder.SampleBarcodeInfo;
import org.openelisglobal.barcode.valueholder.SampleItemBarcodeInfo;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.localization.valueholder.LocalizationValue;

/**
 * Validates barcode ORM mappings without requiring a database connection.
 */
public class HibernateMappingValidationTest {

    @Test
    public void testBarcodeHibernateMappingsLoadSuccessfully() {
        Configuration config = new Configuration();
        config.addAnnotatedClass(SampleBarcodeInfo.class);
        config.addAnnotatedClass(SampleItemBarcodeInfo.class);
        config.addAnnotatedClass(Localization.class);
        config.addAnnotatedClass(LocalizationValue.class);
        config.addResource("hibernate/hbm/Sample.hbm.xml");
        config.addResource("hibernate/hbm/SampleItem.hbm.xml");
        config.addResource("hibernate/hbm/TypeOfSample.hbm.xml");
        config.addResource("hibernate/hbm/UnitOfMeasure.hbm.xml");
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        config.setProperty("hibernate.hbm2ddl.auto", "none");

        SessionFactory sf = config.buildSessionFactory();
        assertNotNull("Barcode Hibernate mappings should load successfully", sf);
        sf.close();
    }
}
