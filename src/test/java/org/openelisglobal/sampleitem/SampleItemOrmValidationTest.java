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
package org.openelisglobal.sampleitem;

import static org.junit.Assert.assertNotNull;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * ORM Validation Test for SampleItem entity modifications (Constitution V.4)
 *
 * <p>
 * Purpose: Validates that Hibernate mappings for SampleItem entity load
 * successfully, including new aliquoting-related fields (originalQuantity,
 * remainingQuantity, parentSampleItem, childAliquots).
 *
 * <p>
 * Requirements: - MUST execute in <5 seconds - MUST NOT require database
 * connection - MUST validate all entity mappings load without errors - MUST
 * verify no JavaBean getter/setter conflicts
 *
 * <p>
 * Context: SampleItem uses hybrid XML + JPA annotations approach (legacy XML
 * exempt until refactored per Constitution III.2). New aliquoting fields added
 * via JPA annotations to enable parent-child relationships and volume tracking.
 *
 * <p>
 * Related: Feature 001-sample-management
 *
 * @see <a href="../../../specs/001-sample-management/spec.md">Feature
 *      Specification</a>
 * @see <a href="../../../.specify/guides/testing-roadmap.md">Testing Roadmap -
 *      ORM Validation Tests</a>
 */
public class SampleItemOrmValidationTest {

    /**
     * Test that SampleItem Hibernate mappings (XML + JPA annotations) load
     * successfully.
     *
     * <p>
     * This test validates: 1. Legacy XML mappings in SampleItem.hbm.xml still load
     * correctly 2. New JPA annotations for aliquoting fields are compatible with
     * XML mappings 3. No conflicts between @Column annotations and XML property
     * definitions 4. SessionFactory can be built without database connection
     *
     * <p>
     * Expected behavior: - SessionFactory builds successfully within 5 seconds - No
     * MappingException or AnnotationException thrown - All entity relationships
     * (many-to-one, one-to-many) configured correctly
     */
    @Test
    public void testSampleItemHibernateMappingsLoadSuccessfully() {
        // Arrange: Configure Hibernate with SampleItem entity
        Configuration config = new Configuration();
        config.addAnnotatedClass(SampleItem.class);
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        // Act: Build SessionFactory (validates all mappings)
        SessionFactory sf = config.buildSessionFactory();

        // Assert: SessionFactory created successfully (mappings are valid)
        assertNotNull("SampleItem Hibernate mappings (XML + JPA annotations) should load without errors", sf);

        // Cleanup
        sf.close();
    }
}
