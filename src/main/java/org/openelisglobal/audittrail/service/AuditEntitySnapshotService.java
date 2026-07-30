package org.openelisglobal.audittrail.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.audittrail.util.AuditFieldStringifier;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the current persisted state of audited entities so the System Audit
 * Events controller can fall back to a snapshot when an Update history row has
 * no per-field diff. Lives in the service layer to keep persistence +
 * reflection out of the controller (constitution: services own
 * {@code @Transactional}).
 *
 * <p>
 * Two public entry points:
 *
 * <ul>
 * <li>{@link #loadSnapshot(String, String)} — empty-Update fallback, restricted
 * via the {@link #SNAPSHOT_FIELDS_BY_REF_TABLE} PII allow-list.
 * <li>{@link #loadFieldValues(String, String, Set)} — explicit-fields lookup
 * used by the per-row "current new value" carry-forward in the patient- scoped
 * path.
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class AuditEntitySnapshotService {

    public static final String PATIENT_ENTITY_NAME = "PATIENT";
    public static final String PERSON_ENTITY_NAME = "PERSON";

    private static final Map<String, Class<?>> REF_TABLE_TO_ENTITY_CLASS = Map.of(PATIENT_ENTITY_NAME, Patient.class,
            PERSON_ENTITY_NAME, Person.class, "TEST", Test.class, "PANEL", Panel.class, "TEST_SECTION",
            TestSection.class, "TYPE_OF_SAMPLE", TypeOfSample.class, "DICTIONARY", Dictionary.class, "analyzer",
            Analyzer.class, "site_information", SiteInformation.class);

    /**
     * Allow-list of entity fields that may appear in the empty-Update fallback
     * snapshot. The fallback fires when an Update row's audit XML has no per-field
     * diff (Hibernate ghost-flush, etc.); without this list, every declared field
     * on the entity would be dumped — which for Patient/Person leaks PII (DOB,
     * addresses, phone, email) into a row that already shows the user just
     * performed an edit.
     *
     * <p>
     * Entries are deliberately minimal: identifiers + display name. Tables not in
     * this map render an empty snapshot, so any newly-audited entity needs an
     * explicit decision here before its fields can surface.
     */
    private static final Map<String, Set<String>> SNAPSHOT_FIELDS_BY_REF_TABLE = Map.of( //
            PATIENT_ENTITY_NAME, Set.of("nationalId", "externalId", "gender"), //
            PERSON_ENTITY_NAME, Set.of("firstName", "lastName"), //
            "TEST", Set.of("description", "loinc"), //
            "PANEL", Set.of("panelName", "description"), //
            "TEST_SECTION", Set.of("testSectionName"), //
            "TYPE_OF_SAMPLE", Set.of("description", "localAbbreviation"), //
            "DICTIONARY", Set.of("dictEntry", "localAbbreviation"), //
            "analyzer", Set.of("name"), //
            "site_information", Set.of("name", "value"));

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Returns a small allow-listed snapshot of the entity's current values, used as
     * a fallback when the audit XML for an Update row has no per-field diff.
     * Restricted by {@link #SNAPSHOT_FIELDS_BY_REF_TABLE} so PII never leaks into
     * rows that already say "edited".
     */
    public Map<String, String> loadSnapshot(String refTableName, String refId) {
        if (refTableName == null || refId == null || refId.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> allowedFields = SNAPSHOT_FIELDS_BY_REF_TABLE.get(refTableName);
        if (allowedFields == null || allowedFields.isEmpty()) {
            return Collections.emptyMap();
        }
        return loadFieldValues(refTableName, refId, allowedFields);
    }

    /**
     * Loads the current persisted entity for {@code (refTableName, refId)} and
     * extracts the named fields' current values, applying
     * {@link AuditFieldStringifier#stringify(Object)} to entity-typed values so
     * they read as a human-friendly name rather than the default
     * {@code ClassName@hash}.
     */
    public Map<String, String> loadFieldValues(String refTableName, String refId, Set<String> fieldNames) {
        if (refTableName == null || refId == null || refId.isEmpty()) {
            return Collections.emptyMap();
        }
        Class<?> entityClass = REF_TABLE_TO_ENTITY_CLASS.get(refTableName);
        if (entityClass == null) {
            return Collections.emptyMap();
        }
        Object entity;
        try {
            entity = entityManager.find(entityClass, refId);
        } catch (RuntimeException e) {
            return Collections.emptyMap();
        }
        if (entity == null) {
            return Collections.emptyMap();
        }
        Map<String, String> current = new HashMap<>();
        for (String fieldName : fieldNames) {
            Field f = findFieldRecursive(entityClass, fieldName);
            if (f == null) {
                continue;
            }
            try {
                f.setAccessible(true);
                Object value = f.get(entity);
                String s = AuditFieldStringifier.stringify(value);
                if (s != null && !s.isEmpty()) {
                    current.put(fieldName, s);
                }
            } catch (IllegalAccessException | RuntimeException e) {
                // skip this field
            }
        }
        return current;
    }

    private Field findFieldRecursive(Class<?> clazz, String fieldName) {
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            try {
                return c.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }
}
