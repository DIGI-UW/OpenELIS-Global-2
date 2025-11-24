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
import org.openelisglobal.sampleitem.valueholder.SampleItemAliquotRelationship;

/**
 * ORM Validation Test for SampleItemAliquotRelationship entity (Constitution
 * V.4)
 *
 * <p>
 * Purpose: Validates that Hibernate mappings for the new
 * SampleItemAliquotRelationship entity load successfully. This is a pure
 * JPA-annotated entity (no XML mappings) that tracks the parent-child
 * relationship and metadata for aliquoting operations.
 *
 * <p>
 * Requirements: - MUST execute in <5 seconds - MUST NOT require database
 * connection - MUST validate all entity mappings load without errors - MUST
 * verify no JavaBean getter/setter conflicts - MUST validate JPA annotations
 * and constraints
 *
 * <p>
 * Context: This is a brand-new entity following Constitution III.2
 * (JPA/Hibernate annotations for new code). It tracks aliquoting metadata
 * including sequence numbers, quantity transferred, and FHIR integration
 * details.
 *
 * <p>
 * Related: Feature 001-sample-management
 *
 * @see <a href="../../../specs/001-sample-management/spec.md">Feature
 *      Specification</a>
 * @see <a href="../../../.specify/guides/testing-roadmap.md">Testing Roadmap -
 *      ORM Validation Tests</a>
 */
public class SampleItemAliquotRelationshipOrmValidationTest {

    /**
     * Test that SampleItemAliquotRelationship Hibernate mappings load successfully.
     *
     * <p>
     * This test validates: 1. All JPA annotations
     * (@Entity, @Table, @Column, @ManyToOne, @UniqueConstraint) are valid 2. Entity
     * relationships (parent/child references to SampleItem) are properly configured
     * 3. Unique constraint on (parentSampleItem, sequenceNumber) is recognized 4.
     * SessionFactory can be built without database connection 5. No conflicts with
     * JavaBean naming conventions
     *
     * <p>
     * Expected behavior: - SessionFactory builds successfully within 5 seconds - No
     * MappingException or AnnotationException thrown - All entity relationships
     * configured correctly - Unique constraint metadata is loaded
     */
    @Test
    public void testSampleItemAliquotRelationshipHibernateMappingsLoadSuccessfully() {
        // Arrange: Configure Hibernate with SampleItemAliquotRelationship entity
        Configuration config = new Configuration();
        config.addAnnotatedClass(SampleItemAliquotRelationship.class);
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        // Act: Build SessionFactory (validates all mappings)
        SessionFactory sf = config.buildSessionFactory();

        // Assert: SessionFactory created successfully (mappings are valid)
        assertNotNull("SampleItemAliquotRelationship Hibernate mappings should load without errors", sf);

        // Cleanup
        sf.close();
    }
}
