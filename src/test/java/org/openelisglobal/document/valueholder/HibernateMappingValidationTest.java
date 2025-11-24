/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.document.valueholder;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * ORM Validation Test - Validates Hibernate mappings load successfully.
 * 
 * This test MUST execute in <5 seconds without a database connection.
 * Catches ORM configuration errors early (getter/setter conflicts, property name mismatches, etc.)
 * 
 * Constitution Principle V.4: ORM Validation Tests
 */
public class HibernateMappingValidationTest {

    @Test
    public void testHibernateMappingsLoadSuccessfully() {
        Configuration config = new Configuration();
        
        // Add annotated entity classes
        config.addAnnotatedClass(IDDocument.class);
        config.addAnnotatedClass(DocumentVersion.class);
        config.addAnnotatedClass(DocumentAudit.class);
        
        // Configure Hibernate for validation (no database needed)
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        config.setProperty("hibernate.hbm2ddl.auto", "validate");
        
        // Build SessionFactory - this will fail if mappings are invalid
        SessionFactory sf = config.buildSessionFactory();
        assertNotNull("All Hibernate mappings should load without errors", sf);
        sf.close();
    }
}

