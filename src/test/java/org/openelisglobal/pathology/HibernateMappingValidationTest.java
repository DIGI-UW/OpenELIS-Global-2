package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.localization.valueholder.LocalizationValue;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.program.valueholder.pathology.CassetteState;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion;
import org.openelisglobal.program.valueholder.pathology.PathologyReport;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.program.valueholder.pathology.PathologyTechnique;

/**
 * Builds the pathology mappings without a database, well inside five seconds,
 * so a broken annotation fails here rather than at application start.
 */
public class HibernateMappingValidationTest {

    private static Metadata metadata;
    private static SessionFactory sessionFactory;

    @BeforeClass
    public static void buildMappings() {
        MetadataSources sources = new MetadataSources(new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.hbm2ddl.auto", "none").build());

        sources.addAnnotatedClass(ProgramSample.class);
        sources.addAnnotatedClass(PathologySample.class);
        sources.addAnnotatedClass(PathologyBlock.class);
        sources.addAnnotatedClass(PathologySlide.class);
        sources.addAnnotatedClass(PathologyRequest.class);
        sources.addAnnotatedClass(PathologyTechnique.class);
        sources.addAnnotatedClass(PathologyConclusion.class);
        sources.addAnnotatedClass(PathologyReport.class);

        // The programme, sample and staff a case points at, and everything their
        // own mappings reach.
        sources.addAnnotatedClass(Localization.class);
        sources.addAnnotatedClass(LocalizationValue.class);
        sources.addResource("hibernate/hbm/Program.hbm.xml");
        sources.addResource("hibernate/hbm/TestSection.hbm.xml");
        sources.addResource("hibernate/hbm/Organization.hbm.xml");
        sources.addResource("hibernate/hbm/OrganizationType.hbm.xml");
        sources.addResource("hibernate/hbm/SystemUser.hbm.xml");
        sources.addResource("hibernate/hbm/Sample.hbm.xml");

        metadata = sources.buildMetadata();
        sessionFactory = metadata.buildSessionFactory();
    }

    @AfterClass
    public static void closeSessionFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    /** Every pathology entity is registered, so no annotation is unreadable. */
    @Test
    public void pathologyMappings_buildWithoutADatabase() {
        assertNotNull("the pathology mappings build a SessionFactory with no database behind them", sessionFactory);

        for (Class<?> entity : Arrays.asList(ProgramSample.class, PathologySample.class, PathologyBlock.class,
                PathologySlide.class, PathologyRequest.class, PathologyTechnique.class, PathologyConclusion.class,
                PathologyReport.class)) {
            assertNotNull(entity.getSimpleName() + " is registered in the metamodel",
                    sessionFactory.getMetamodel().entity(entity));
        }
    }

    /** The identity a cassette carries reaches the columns the schema declares. */
    @Test
    public void pathologyBlock_mapsTheIdentityColumns() {
        EntityType<PathologyBlock> block = sessionFactory.getMetamodel().entity(PathologyBlock.class);

        assertEquals("designation is a String", String.class, block.getAttribute("designation").getJavaType());
        assertEquals("barcode is a String", String.class, block.getAttribute("barcode").getJavaType());
        assertEquals("cassetteState is the CassetteState enum", CassetteState.class,
                block.getAttribute("cassetteState").getJavaType());
        assertEquals("tissueTypeId is a String, as dictionary ids are throughout", String.class,
                block.getAttribute("tissueTypeId").getJavaType());
        assertEquals("partDesignation is a String", String.class, block.getAttribute("partDesignation").getJavaType());

        Attribute<? super PathologyBlock, ?> active = block.getAttribute("active");
        assertEquals("active is a primitive boolean, so a row is active or it is not", boolean.class,
                active.getJavaType());
        assertEquals("active is a basic attribute, not an association", Attribute.PersistentAttributeType.BASIC,
                active.getPersistentAttributeType());

        assertColumn(PathologyBlock.class, "designation", "designation");
        assertColumn(PathologyBlock.class, "barcode", "barcode");
        assertColumn(PathologyBlock.class, "cassetteState", "cassette_state");
        assertColumn(PathologyBlock.class, "tissueTypeId", "tissue_type_id");
        assertColumn(PathologyBlock.class, "partDesignation", "part_designation");
        assertColumn(PathologyBlock.class, "active", "active");
    }

    /** A slide records the block it was cut from and its own identity. */
    @Test
    public void pathologySlide_mapsItsParentBlock() {
        EntityType<PathologySlide> slide = sessionFactory.getMetamodel().entity(PathologySlide.class);

        assertEquals("blockId is an Integer, matching the block id type", Integer.class,
                slide.getAttribute("blockId").getJavaType());
        assertEquals("blockId is a plain column, not an association", Attribute.PersistentAttributeType.BASIC,
                slide.getAttribute("blockId").getPersistentAttributeType());
        assertEquals("active is a primitive boolean", boolean.class, slide.getAttribute("active").getJavaType());

        assertColumn(PathologySlide.class, "blockId", "block_id");
        assertColumn(PathologySlide.class, "designation", "designation");
        assertColumn(PathologySlide.class, "barcode", "barcode");
        assertColumn(PathologySlide.class, "level", "level");
        assertColumn(PathologySlide.class, "stainId", "stain_id");
        assertColumn(PathologySlide.class, "stainStatus", "stain_status");
        assertColumn(PathologySlide.class, "active", "active");
    }

    /**
     * Dropping a block or slide from its collection must not delete the row, which
     * 42 CFR 493.1105 requires the laboratory to retain.
     */
    @Test
    public void pathologySample_blocksAndSlidesDoNotRemoveOrphans() {
        PersistentClass caseBinding = metadata.getEntityBinding(PathologySample.class.getName());

        for (String collection : Arrays.asList("blocks", "slides")) {
            Property property = caseBinding.getProperty(collection);
            org.hibernate.mapping.Collection binding = (org.hibernate.mapping.Collection) property.getValue();

            assertFalse(
                    "PathologySample." + collection + " must not delete orphans: a row dropped from the collection"
                            + " would be deleted, and 42 CFR 493.1105 requires blocks and slides to be retained",
                    binding.hasOrphanDelete());
            assertTrue("PathologySample." + collection + " still cascades all, so a child saves with its case",
                    property.getCascade().contains("all"));
        }
    }

    /**
     * One accessor for active, because Hibernate cannot choose between
     * {@code isActive()} and {@code getActive()}.
     */
    @Test
    public void pathologyBlock_hasOneAccessorForActive() {
        for (Class<?> entity : Arrays.asList(PathologyBlock.class, PathologySlide.class)) {
            assertTrue(entity.getSimpleName() + " declares isActive()", declaresGetter(entity, "isActive"));
            assertFalse(entity.getSimpleName() + " must not also declare getActive(): two accessors for one property"
                    + " leave Hibernate no way to choose", declaresGetter(entity, "getActive"));
        }
    }

    private static void assertColumn(Class<?> entity, String attribute, String expectedColumn) {
        PersistentClass binding = metadata.getEntityBinding(entity.getName());
        Iterator<?> columns = binding.getProperty(attribute).getValue().getColumnIterator();

        assertTrue(entity.getSimpleName() + "." + attribute + " maps to a column", columns.hasNext());
        assertEquals(entity.getSimpleName() + "." + attribute + " maps to the column the schema declares",
                expectedColumn, ((Column) columns.next()).getName());
        assertFalse(entity.getSimpleName() + "." + attribute + " maps to one column only", columns.hasNext());
    }

    private static boolean declaresGetter(Class<?> entity, String name) {
        List<Method> declared = Arrays.asList(entity.getDeclaredMethods());
        return declared.stream().anyMatch(m -> m.getName().equals(name) && m.getParameterCount() == 0);
    }
}
