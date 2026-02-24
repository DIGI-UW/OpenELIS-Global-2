package org.openelisglobal.qaevent.valueholder;

import static org.junit.Assert.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import org.junit.Test;
import org.openelisglobal.qaevent.valueholder.NceResultAssociation.AssociationType;
import org.openelisglobal.result.valueholder.Result;

/**
 * ORM validation tests for NceResultAssociation entity. Verifies JPA
 * annotations are correctly applied without requiring a database connection.
 * Must execute in less than 5 seconds per Constitution V.4.
 */
public class NceResultAssociationOrmTest {

    @Test
    public void entityAnnotation_isPresent() {
        assertTrue("@Entity annotation is required", NceResultAssociation.class.isAnnotationPresent(Entity.class));
    }

    @Test
    public void tableAnnotation_mapsToCorrectTable() {
        Table table = NceResultAssociation.class.getAnnotation(Table.class);
        assertNotNull("@Table annotation is required", table);
        assertEquals("nce_result_association", table.name());
    }

    @Test
    public void idField_hasCorrectAnnotations() throws NoSuchFieldException {
        Field idField = NceResultAssociation.class.getDeclaredField("id");

        assertNotNull("@Id annotation is required", idField.getAnnotation(Id.class));
        GeneratedValue genVal = idField.getAnnotation(GeneratedValue.class);
        assertNotNull("@GeneratedValue annotation is required", genVal);
        assertEquals(GenerationType.IDENTITY, genVal.strategy());

        Column col = idField.getAnnotation(Column.class);
        assertNotNull("@Column annotation is required", col);
        assertEquals("id", col.name());
    }

    @Test
    public void resultIdField_mapsToCorrectColumn() throws NoSuchFieldException {
        Field field = NceResultAssociation.class.getDeclaredField("resultId");

        Column col = field.getAnnotation(Column.class);
        assertNotNull("@Column annotation is required", col);
        assertEquals("result_id", col.name());
    }

    @Test
    public void ncEventField_hasLazyManyToOne() throws NoSuchFieldException {
        Field field = NceResultAssociation.class.getDeclaredField("ncEvent");

        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        assertNotNull("@ManyToOne annotation is required", manyToOne);
        assertEquals(FetchType.LAZY, manyToOne.fetch());

        JoinColumn joinCol = field.getAnnotation(JoinColumn.class);
        assertNotNull("@JoinColumn annotation is required", joinCol);
        assertEquals("nce_id", joinCol.name());
    }

    @Test
    public void associationTypeField_mapsToCorrectColumn() throws NoSuchFieldException {
        Field field = NceResultAssociation.class.getDeclaredField("associationType");
        Column col = field.getAnnotation(Column.class);
        assertNotNull("@Column annotation is required", col);
        assertEquals("association_type", col.name());
        assertEquals(50, col.length());
    }

    @Test
    public void createdByField_mapsToCorrectColumn() throws NoSuchFieldException {
        Field field = NceResultAssociation.class.getDeclaredField("createdBy");
        Column col = field.getAnnotation(Column.class);
        assertNotNull("@Column annotation is required", col);
        assertEquals("created_by", col.name());
        assertEquals(100, col.length());
    }

    @Test
    public void descriptionField_mapsToCorrectColumn() throws NoSuchFieldException {
        Field field = NceResultAssociation.class.getDeclaredField("description");
        Column col = field.getAnnotation(Column.class);
        assertNotNull("@Column annotation is required", col);
        assertEquals("description", col.name());
    }

    @Test
    public void parameterizedConstructor_setsAllFields() {
        Result result = new Result();
        result.setId("result-1");
        NcEvent ncEvent = new NcEvent();
        ncEvent.setId("1");

        NceResultAssociation assoc = new NceResultAssociation(result.getId(), ncEvent,
                AssociationType.RESULT_TRIGGERED_NCE, "admin");

        assertEquals("result-1", assoc.getResultId());
        assertEquals(ncEvent, assoc.getNcEvent());
        assertEquals("RESULT_TRIGGERED_NCE", assoc.getAssociationType());
        assertEquals("admin", assoc.getCreatedBy());
        assertNotNull(assoc.getCreatedDate());
    }

    @Test
    public void associationTypeEnumConversion_roundTrip() {
        NceResultAssociation assoc = new NceResultAssociation();

        for (AssociationType type : AssociationType.values()) {
            assoc.setAssociationTypeEnum(type);
            assertEquals(type.name(), assoc.getAssociationType());
            assertEquals(type, assoc.getAssociationTypeEnum());
        }
    }

    @Test
    public void associationTypeEnum_hasExpectedValues() {
        assertEquals(4, AssociationType.values().length);
        assertNotNull(AssociationType.RESULT_TRIGGERED_NCE);
        assertNotNull(AssociationType.RESULT_AFFECTED_BY_NCE);
        assertNotNull(AssociationType.RESULT_PART_OF_NCE);
        assertNotNull(AssociationType.DELTA_CHECK_ESCALATION);
    }

    @Test
    public void associationTypeEnum_allHaveDescriptions() {
        for (AssociationType type : AssociationType.values()) {
            assertNotNull("AssociationType " + type.name() + " should have a description", type.getDescription());
            assertFalse("AssociationType " + type.name() + " description should not be empty",
                    type.getDescription().isEmpty());
        }
    }

    @Test
    public void equalsAndHashCode_sameId_areEqual() {
        NceResultAssociation a1 = new NceResultAssociation();
        a1.setId(1);
        NceResultAssociation a2 = new NceResultAssociation();
        a2.setId(1);

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    @Test
    public void equalsAndHashCode_differentId_notEqual() {
        NceResultAssociation a1 = new NceResultAssociation();
        a1.setId(1);
        NceResultAssociation a2 = new NceResultAssociation();
        a2.setId(2);

        assertNotEquals(a1, a2);
    }

    @Test
    public void toString_containsKeyFields() {
        NceResultAssociation assoc = new NceResultAssociation();
        assoc.setId(1);
        assoc.setAssociationType("RESULT_TRIGGERED_NCE");
        assoc.setCreatedBy("admin");

        String str = assoc.toString();
        assertTrue(str.contains("id=1"));
        assertTrue(str.contains("RESULT_TRIGGERED_NCE"));
        assertTrue(str.contains("admin"));
    }
}
