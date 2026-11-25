package org.openelisglobal.reports.dataexport;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.userrole.valueholder.UserRole;

/**
 * Where a test order came from, as Organization Management records it
 * (OGC-1652): Alotau Hospital (code ALT-01) in Milne Bay District, Southern
 * Region, and its Maternity Ward, attached to the order as its requesting
 * facility and ward. The area types carry no hierarchy level, as on a standard
 * installation. Written inside the test's own transaction.
 */
final class ExportLocationFixture {

    static final String REGION_TYPE = "99711";
    static final String DISTRICT_TYPE = "99712";
    static final String REGION = "99701";
    private static final String DISTRICT = "99702";
    private static final String FACILITY = "99703";
    private static final String WARD = "99704";

    private ExportLocationFixture() {
    }

    static void attach(EntityManager entityManager, String sampleId) {
        String clinic = clinicType(entityManager);
        String dept = typeId(entityManager, "dept", "99714");
        insertType(entityManager, REGION_TYPE, "Health Region", null);
        insertType(entityManager, DISTRICT_TYPE, "Health District", null);
        organization(entityManager, REGION, "Southern Region", null, null, REGION_TYPE);
        organization(entityManager, DISTRICT, "Milne Bay District", REGION, null, DISTRICT_TYPE);
        organization(entityManager, FACILITY, "Alotau Hospital", DISTRICT, "ALT-01", clinic);
        organization(entityManager, WARD, "Maternity Ward", FACILITY, null, dept);
        Object organizationRequester = entityManager
                .createNativeQuery(
                        "SELECT id FROM clinlims.requester_type WHERE lower(requester_type) = 'organization'")
                .getSingleResult();
        requester(entityManager, "99721", sampleId, FACILITY, organizationRequester);
        requester(entityManager, "99722", sampleId, WARD, organizationRequester);
    }

    static String clinicType(EntityManager entityManager) {
        return typeId(entityManager, "referring clinic", "99713");
    }

    private static String typeId(EntityManager entityManager, String name, String newId) {
        List<?> found = entityManager
                .createNativeQuery("SELECT id FROM clinlims.organization_type WHERE lower(short_name) = :name")
                .setParameter("name", name).getResultList();
        if (!found.isEmpty()) {
            return String.valueOf(found.get(0));
        }
        insertType(entityManager, newId, name, null);
        return newId;
    }

    static void insertType(EntityManager entityManager, String id, String name, Integer level) {
        entityManager
                .createNativeQuery("INSERT INTO clinlims.organization_type (id, short_name, description,"
                        + " hierarchy_level, lastupdated) VALUES (CAST(:id AS numeric), :name, :name, "
                        + (level == null ? "NULL" : level.toString()) + ", now())")
                .setParameter("id", id).setParameter("name", name).executeUpdate();
    }

    static void organization(EntityManager entityManager, String id, String name, String parent, String code,
            String type) {
        entityManager
                .createNativeQuery("INSERT INTO clinlims.organization (id, name, org_id, code, is_active,"
                        + " mls_sentinel_lab_flag, lastupdated) VALUES (CAST(:id AS numeric), :name, "
                        + (parent == null ? "NULL" : "CAST('" + parent + "' AS numeric)") + ", "
                        + (code == null ? "NULL" : "'" + code + "'") + ", 'Y', 'N', now())")
                .setParameter("id", id).setParameter("name", name).executeUpdate();
        entityManager
                .createNativeQuery("INSERT INTO clinlims.organization_organization_type (org_id, org_type_id)"
                        + " VALUES (CAST(:id AS numeric), CAST(:type AS numeric))")
                .setParameter("id", id).setParameter("type", type).executeUpdate();
    }

    private static void requester(EntityManager entityManager, String id, String sampleId, String organization,
            Object requesterType) {
        entityManager
                .createNativeQuery("INSERT INTO clinlims.sample_requester (id, sample_id, requester_id,"
                        + " requester_type_id, lastupdated) VALUES (CAST(:id AS numeric), CAST(:sample AS numeric),"
                        + " CAST(:organization AS numeric), :type, now())")
                .setParameter("id", id).setParameter("sample", sampleId).setParameter("organization", organization)
                .setParameter("type", requesterType).executeUpdate();
    }

    /**
     * A reporting user who may see every lab unit, made for the test, so the test
     * does not depend on another test's changes to the admin user's roles.
     */
    static String reportingAdmin(EntityManager entityManager) {
        var role = entityManager.createQuery("from Role where name = :name", Role.class)
                .setParameter("name", Constants.ROLE_GLOBAL_ADMIN).getResultStream().findFirst().orElseGet(() -> {
                    var created = new Role();
                    created.setName(Constants.ROLE_GLOBAL_ADMIN);
                    created.setActive(true);
                    entityManager.persist(created);
                    return created;
                });
        var user = new SystemUser();
        user.setLoginName("reporting-" + UUID.randomUUID().toString().substring(0, 8));
        user.setFirstName("Reporting");
        user.setLastName("Admin");
        user.setIsActive("Y");
        user.setIsEmployee("Y");
        entityManager.persist(user);
        var grant = new UserRole();
        grant.setSystemUserId(user.getId());
        grant.setRoleId(role.getId());
        entityManager.persist(grant);
        entityManager.flush();
        return user.getId();
    }
}
