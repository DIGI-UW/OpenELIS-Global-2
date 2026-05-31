package org.openelisglobal.labelpreset.valueholder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.PluralAttribute;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.type.CustomType;
import org.hibernate.type.Type;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.hibernate.type.JsonbObjectType;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * ORM validation test for the 5 OGC-285 label-preset value-holders
 * (Constitution V.4 / data-model.md §5). Validates the {@code @Entity} →
 * {@code @Table} mapping for every entity: every persistent field maps to a
 * real column, every relationship resolves to its target entity, and the JSONB
 * {@link Type} binds to the typed {@link JsonbObjectType} (NOT a
 * serializable-blob fallback).
 *
 * <p>
 * <strong>Why DB-backed instead of a standalone no-DB
 * {@code Configuration}.</strong> tasks.md T025 sketches a no-DB Hibernate
 * {@code Metamodel} build (the {@code barcode}/{@code PatientMergeAudit}
 * idiom). That idiom works only for entities whose FK closure is shallow. Three
 * of these entities ({@code OrderLabelRequest}, {@code TestLabelConfig},
 * {@code TestLabelPresetLink}) hold FKs into the legacy {@code .hbm.xml}-mapped
 * {@code Test}, and {@code Test.hbm.xml} drags in a deep transitive closure
 * ({@code TestSection} → {@code Organization}, plus collection mappings on
 * {@code TestResult}/{@code TestTrailer}/…) that fails during Hibernate's
 * collection second-pass binding — a no-DB build does NOT converge. Per the
 * task directive to follow the proven pattern rather than force a no-DB build
 * that won't run, this test unwraps the real, production-configured
 * {@link SessionFactory} from the Spring {@link EntityManagerFactory} (which
 * already harmonizes the annotated + hbm graph exactly as production does — the
 * same mechanism that lets {@code SampleBarcodeInfo} reference the hbm-mapped
 * {@code Sample}). It therefore runs in the integration tier, not {@code <5s}.
 *
 * <p>
 * Lifecycle and the JavaBean getter-conflict check mirror the cited
 * {@code PatientMergeAuditHibernateMappingValidationTest}; the persister-based
 * column pinning + JSONB-type assertion make the test inversion-worthy: a
 * {@code @Column}/{@code @JoinColumn} rename or a removed
 * {@code @Type("jsonb")} turns it RED even though the SessionFactory would
 * still build.
 *
 * <p>
 * Scope guard: this asserts mapping consistency and pins the <em>intended</em>
 * column names/types against the live mapping metadata. Byte-level JSONB
 * persistence round-trip is the separate DB-backed
 * {@code PresetSnapshotJsonbRoundtripTest} (T026).
 *
 * <p>
 * Task: T025 [US1] — ORM validation for label-preset entities.
 */
public class LabelPresetOrmValidationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private SessionFactory sessionFactory;

    @Before
    public void unwrapSessionFactory() {
        sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    }

    // ---------------------------------------------------------------------
    // 1. All 5 entities registered in the metamodel.
    // ---------------------------------------------------------------------

    @Test
    public void allFiveEntitiesRegisteredInMetamodel() {
        Metamodel metamodel = sessionFactory.getMetamodel();
        assertNotNull("LabelPreset should be a registered entity", metamodel.entity(LabelPreset.class));
        assertNotNull("LabelPresetField should be a registered entity", metamodel.entity(LabelPresetField.class));
        assertNotNull("TestLabelConfig should be a registered entity", metamodel.entity(TestLabelConfig.class));
        assertNotNull("TestLabelPresetLink should be a registered entity", metamodel.entity(TestLabelPresetLink.class));
        assertNotNull("OrderLabelRequest should be a registered entity", metamodel.entity(OrderLabelRequest.class));
    }

    // ---------------------------------------------------------------------
    // 2. Relationships resolve to the expected target entity types.
    // Catches a wrong/renamed @ManyToOne / @OneToOne / @OneToMany target.
    // ---------------------------------------------------------------------

    @Test
    public void labelPresetFieldRelationshipsResolve() {
        assertAttributeTargetType(LabelPresetField.class, "preset", LabelPreset.class);
    }

    @Test
    public void labelPresetFieldsCollectionResolves() {
        EntityType<LabelPreset> labelPreset = sessionFactory.getMetamodel().entity(LabelPreset.class);
        Attribute<? super LabelPreset, ?> fields = labelPreset.getAttribute("fields");
        assertTrue("LabelPreset.fields should be a collection association", fields.isCollection());
        assertEquals("LabelPreset.fields should hold LabelPresetField", LabelPresetField.class,
                ((PluralAttribute<?, ?, ?>) fields).getElementType().getJavaType());
    }

    @Test
    public void testLabelConfigRelationshipsResolve() {
        assertAttributeTargetType(TestLabelConfig.class, "test", org.openelisglobal.test.valueholder.Test.class);
    }

    @Test
    public void testLabelPresetLinkRelationshipsResolve() {
        assertAttributeTargetType(TestLabelPresetLink.class, "test", org.openelisglobal.test.valueholder.Test.class);
        assertAttributeTargetType(TestLabelPresetLink.class, "preset", LabelPreset.class);
    }

    @Test
    public void orderLabelRequestRelationshipsResolve() {
        assertAttributeTargetType(OrderLabelRequest.class, "parentSample", Sample.class);
        assertAttributeTargetType(OrderLabelRequest.class, "sampleItem", SampleItem.class);
        assertAttributeTargetType(OrderLabelRequest.class, "preset", LabelPreset.class);
    }

    // ---------------------------------------------------------------------
    // 3. Every persistent field maps to its intended column name.
    // Pins @Column / @JoinColumn names so a rename/typo goes RED.
    // ---------------------------------------------------------------------

    @Test
    public void labelPresetColumnsPinned() {
        AbstractEntityPersister persister = persister(LabelPreset.class);
        assertColumn(persister, "name", "name");
        assertColumn(persister, "heightMm", "height_mm");
        assertColumn(persister, "widthMm", "width_mm");
        assertColumn(persister, "barcodeType", "barcode_type");
        assertColumn(persister, "printsPerOrder", "prints_per_order");
        assertColumn(persister, "printsPerSample", "prints_per_sample");
        assertColumn(persister, "defaultPerOrder", "default_per_order");
        assertColumn(persister, "maxPerOrder", "max_per_order");
        assertColumn(persister, "defaultPerSample", "default_per_sample");
        assertColumn(persister, "maxPerSample", "max_per_sample");
        assertColumn(persister, "isSystem", "is_system");
        assertColumn(persister, "isActive", "is_active");
    }

    @Test
    public void labelPresetFieldColumnsPinned() {
        AbstractEntityPersister persister = persister(LabelPresetField.class);
        assertColumn(persister, "preset", "preset_id");
        assertColumn(persister, "fieldKey", "field_key");
        assertColumn(persister, "sourceType", "source_type");
        assertColumn(persister, "isRequired", "is_required");
        assertColumn(persister, "displayOrder", "display_order");
    }

    @Test
    public void testLabelConfigColumnsPinned() {
        AbstractEntityPersister persister = persister(TestLabelConfig.class);
        assertColumn(persister, "test", "test_id");
        assertColumn(persister, "allowOrderEntryOverride", "allow_order_entry_override");
    }

    @Test
    public void testLabelPresetLinkColumnsPinned() {
        AbstractEntityPersister persister = persister(TestLabelPresetLink.class);
        assertColumn(persister, "test", "test_id");
        assertColumn(persister, "preset", "preset_id");
        assertColumn(persister, "defaultQty", "default_qty");
        assertColumn(persister, "maxQty", "max_qty");
        assertColumn(persister, "allowOverride", "allow_override");
    }

    @Test
    public void orderLabelRequestColumnsPinned() {
        AbstractEntityPersister persister = persister(OrderLabelRequest.class);
        assertColumn(persister, "parentSample", "parent_sample_id");
        assertColumn(persister, "sampleItem", "sample_item_id");
        assertColumn(persister, "preset", "preset_id");
        assertColumn(persister, "qty", "qty");
        assertColumn(persister, "presetSnapshot", "preset_snapshot");
    }

    // ---------------------------------------------------------------------
    // 4. The JSONB column binds to the typed JsonbObjectType UserType.
    // Removing @Type(type="jsonb-object") would fall back to a serializable blob
    // (PresetSnapshotDto is Serializable) and the SF would still build —
    // so assert the bound type explicitly to keep this inversion-worthy.
    // ---------------------------------------------------------------------

    @Test
    public void orderLabelRequestPresetSnapshotBindsJsonbObjectType() {
        AbstractEntityPersister persister = persister(OrderLabelRequest.class);
        Type snapshotType = persister.getPropertyType("presetSnapshot");
        assertNotNull("presetSnapshot must have a mapped Hibernate type", snapshotType);
        // @Type(type="jsonb-object") binds the column via a CustomType wrapping the
        // typed JsonbObjectType. If @Type were removed, Hibernate would fall back
        // to a SerializableType (PresetSnapshotDto is Serializable) — NOT a
        // CustomType — and this assertion goes RED.
        assertTrue(
                "preset_snapshot must bind via a CustomType (@Type-driven UserType), not a serializable-blob"
                        + " fallback; actual Hibernate type was " + snapshotType.getClass().getName(),
                snapshotType instanceof CustomType);
        assertEquals("preset_snapshot must bind to the typed JsonbObjectType UserType, not another UserType",
                JsonbObjectType.class, underlyingUserType(snapshotType));
    }

    // ---------------------------------------------------------------------
    // 5. JavaBean getter-hygiene (mirrors PatientMergeAudit reference test):
    // no getX()/isX() conflict that would make Hibernate ambiguous.
    // ---------------------------------------------------------------------

    @Test
    public void entitiesHaveNoGetterConflicts() {
        validateNoGetterConflicts(LabelPreset.class);
        validateNoGetterConflicts(LabelPresetField.class);
        validateNoGetterConflicts(TestLabelConfig.class);
        validateNoGetterConflicts(TestLabelPresetLink.class);
        validateNoGetterConflicts(OrderLabelRequest.class);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private AbstractEntityPersister persister(Class<?> entityClass) {
        MetamodelImplementor metamodel = (MetamodelImplementor) sessionFactory.getMetamodel();
        return (AbstractEntityPersister) metamodel.entityPersister(entityClass);
    }

    private void assertColumn(AbstractEntityPersister persister, String property, String expectedColumn) {
        String[] columns = persister.getPropertyColumnNames(property);
        assertArrayEquals(persister.getEntityName() + "." + property + " should map to column [" + expectedColumn + "]",
                new String[] { expectedColumn }, columns);
    }

    private void assertAttributeTargetType(Class<?> owner, String attribute, Class<?> expectedTarget) {
        EntityType<?> entityType = sessionFactory.getMetamodel().entity(owner);
        Attribute<?, ?> attr = entityType.getAttribute(attribute);
        assertNotNull(owner.getSimpleName() + "." + attribute + " attribute should exist", attr);
        assertTrue(owner.getSimpleName() + "." + attribute + " should be an association", attr.isAssociation());
        assertEquals(owner.getSimpleName() + "." + attribute + " should target " + expectedTarget.getSimpleName(),
                expectedTarget, attr.getJavaType());
    }

    /**
     * Unwraps the Hibernate {@code Type} for the JSONB column to the underlying
     * {@code UserType} class. This JSONB binding wraps {@link JsonbObjectType} in a
     * {@link CustomType}; if {@code @Type(type="jsonb-object")} were removed, this
     * would instead be a {@code SerializableType} (PresetSnapshotDto is
     * Serializable), and the equality assertion fails with a descriptive mismatch.
     */
    private Class<?> underlyingUserType(Type type) {
        if (type instanceof CustomType) {
            return ((CustomType) type).getUserType().getClass();
        }
        return type.getClass();
    }

    private void validateNoGetterConflicts(Class<?> clazz) {
        Map<String, Method> getGetters = new HashMap<>();
        Map<String, Method> isGetters = new HashMap<>();

        for (Method method : clazz.getMethods()) {
            if (method.getName().startsWith("get") && method.getParameterCount() == 0
                    && !method.getName().equals("getClass")) {
                getGetters.put(decapitalize(method.getName().substring(3)), method);
            }
            if (method.getName().startsWith("is") && method.getParameterCount() == 0) {
                isGetters.put(decapitalize(method.getName().substring(2)), method);
            }
        }

        Set<String> conflicts = new HashSet<>(getGetters.keySet());
        conflicts.retainAll(isGetters.keySet());

        if (!conflicts.isEmpty()) {
            StringBuilder message = new StringBuilder(clazz.getSimpleName())
                    .append(" has conflicting getters for properties: ");
            for (String prop : conflicts) {
                Method getMeth = getGetters.get(prop);
                Method isMeth = isGetters.get(prop);
                message.append("\n  - ").append(prop).append(": ").append(getMeth.getName()).append("() returning ")
                        .append(getMeth.getReturnType().getSimpleName()).append(" vs ").append(isMeth.getName())
                        .append("() returning ").append(isMeth.getReturnType().getSimpleName());
            }
            message.append("\n  Hibernate cannot determine which getter to use.");
            fail(message.toString());
        }
    }

    private String decapitalize(String string) {
        if (string == null || string.isEmpty()) {
            return string;
        }
        return string.substring(0, 1).toLowerCase() + string.substring(1);
    }
}
