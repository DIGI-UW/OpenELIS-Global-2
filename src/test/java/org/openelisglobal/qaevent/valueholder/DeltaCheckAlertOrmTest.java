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
import java.math.BigDecimal;
import org.junit.Test;

/**
 * ORM validation tests for DeltaCheckAlert entity. Verifies JPA annotations are
 * correctly applied without requiring a database connection. Must execute in
 * less than 5 seconds per Constitution V.4.
 */
public class DeltaCheckAlertOrmTest {

    @Test
    public void entityAnnotation_isPresent() {
        assertTrue("@Entity annotation is required", DeltaCheckAlert.class.isAnnotationPresent(Entity.class));
    }

    @Test
    public void tableAnnotation_mapsToCorrectTable() {
        Table table = DeltaCheckAlert.class.getAnnotation(Table.class);
        assertNotNull("@Table annotation is required", table);
        assertEquals("delta_check_alert", table.name());
    }

    @Test
    public void idField_hasCorrectAnnotations() throws NoSuchFieldException {
        Field idField = DeltaCheckAlert.class.getDeclaredField("id");

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
        Field resultField = DeltaCheckAlert.class.getDeclaredField("resultId");

        Column col = resultField.getAnnotation(Column.class);
        assertNotNull("@Column annotation is required", col);
        assertEquals("result_id", col.name());
    }

    @Test
    public void previousResultIdField_mapsToCorrectColumn() throws NoSuchFieldException {
        Field field = DeltaCheckAlert.class.getDeclaredField("previousResultId");

        Column col = field.getAnnotation(Column.class);
        assertNotNull("@Column annotation is required", col);
        assertEquals("previous_result_id", col.name());
    }

    @Test
    public void escalatedNcEventField_hasLazyManyToOne() throws NoSuchFieldException {
        Field field = DeltaCheckAlert.class.getDeclaredField("escalatedNcEvent");

        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        assertNotNull("@ManyToOne annotation is required", manyToOne);
        assertEquals(FetchType.LAZY, manyToOne.fetch());

        JoinColumn joinCol = field.getAnnotation(JoinColumn.class);
        assertNotNull("@JoinColumn annotation is required", joinCol);
        assertEquals("escalated_nce_id", joinCol.name());
    }

    @Test
    public void statusField_mapsToCorrectColumn() throws NoSuchFieldException {
        Field field = DeltaCheckAlert.class.getDeclaredField("status");
        Column col = field.getAnnotation(Column.class);
        assertNotNull("@Column annotation is required", col);
        assertEquals("status", col.name());
        assertEquals(20, col.length());
    }

    @Test
    public void changePercentField_mapsToCorrectColumn() throws NoSuchFieldException {
        Field field = DeltaCheckAlert.class.getDeclaredField("changePercent");
        Column col = field.getAnnotation(Column.class);
        assertNotNull("@Column annotation is required", col);
        assertEquals("change_percent", col.name());
    }

    @Test
    public void thresholdPercentField_mapsToCorrectColumn() throws NoSuchFieldException {
        Field field = DeltaCheckAlert.class.getDeclaredField("thresholdPercent");
        Column col = field.getAnnotation(Column.class);
        assertNotNull("@Column annotation is required", col);
        assertEquals("threshold_percent", col.name());
    }

    @Test
    public void defaultConstructor_setsActiveStatusAndCreatedDate() {
        DeltaCheckAlert alert = new DeltaCheckAlert();

        assertEquals("ACTIVE", alert.getStatus());
        assertTrue(alert.isActive());
        assertNotNull(alert.getCreatedDate());
    }

    @Test
    public void parameterizedConstructor_calculatesChangePercent() {
        DeltaCheckAlert alert = new DeltaCheckAlert(null, null, new BigDecimal("150"), new BigDecimal("100"),
                new BigDecimal("25.00"));

        assertEquals(0, new BigDecimal("50.00").compareTo(alert.getChangePercent()));
        assertEquals(new BigDecimal("25.00"), alert.getThresholdPercent());
    }

    @Test
    public void dismiss_setsStatusAndFields() {
        DeltaCheckAlert alert = new DeltaCheckAlert();
        alert.dismiss("Expected variation", "admin");

        assertEquals("DISMISSED", alert.getStatus());
        assertTrue(alert.isDismissed());
        assertFalse(alert.isActive());
        assertEquals("Expected variation", alert.getDismissalReason());
        assertEquals("admin", alert.getDismissedBy());
        assertNotNull(alert.getDismissedDate());
    }

    @Test
    public void escalateToNCE_setsStatusAndNcEvent() {
        DeltaCheckAlert alert = new DeltaCheckAlert();
        NcEvent ncEvent = new NcEvent();
        ncEvent.setId("1");

        alert.escalateToNCE(ncEvent);

        assertEquals("ESCALATED_NCE", alert.getStatus());
        assertTrue(alert.isEscalatedToNCE());
        assertFalse(alert.isActive());
        assertEquals(ncEvent, alert.getEscalatedNcEvent());
    }

    @Test
    public void alertStatusEnum_hasExpectedValues() {
        assertEquals(3, DeltaCheckAlert.AlertStatus.values().length);
        assertNotNull(DeltaCheckAlert.AlertStatus.ACTIVE);
        assertNotNull(DeltaCheckAlert.AlertStatus.DISMISSED);
        assertNotNull(DeltaCheckAlert.AlertStatus.ESCALATED_NCE);

        for (DeltaCheckAlert.AlertStatus status : DeltaCheckAlert.AlertStatus.values()) {
            assertNotNull(status.getDescription());
            assertFalse(status.getDescription().isEmpty());
        }
    }

    @Test
    public void getStatusEnum_invalidString_returnsActiveDefault() {
        DeltaCheckAlert alert = new DeltaCheckAlert();
        alert.setStatus("INVALID_STATUS");

        assertEquals(DeltaCheckAlert.AlertStatus.ACTIVE, alert.getStatusEnum());
    }
}
