package org.openelisglobal.dataexchange.order.daoimpl;

import static org.junit.Assert.assertNotNull;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;

/**
 * Validates that all refactored HQL queries in ElectronicOrderDAOImpl
 * successfully parse and compile against Hibernate's metamodel and entity
 * mappings without requiring a running database or Docker container.
 */
public class ElectronicOrderHqlValidationTest {

    private static SessionFactory sessionFactory;

    @BeforeClass
    public static void setUp() {
        Configuration configuration = new Configuration();
        configuration.addResource("hibernate/hbm/Person.hbm.xml");
        configuration.addResource("hibernate/hbm/Patient.hbm.xml");
        configuration.addResource("hibernate/hbm/PatientIdentityType.hbm.xml");
        configuration.addResource("hibernate/hbm/PatientIdentity.hbm.xml");
        configuration.addResource("hibernate/hbm/ElectronicOrder.hbm.xml");

        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "none");
        configuration.setProperty("hibernate.search.backend.type", "lucene");
        configuration.setProperty("hibernate.search.backend.directory.type", "local-heap");
        configuration.setProperty("hibernate.search.backend.analysis.configurer",
                "class:org.openelisglobal.hibernate.search.analysis.CustomLuceneAnalysisConfigurer");

        sessionFactory = configuration.buildSessionFactory(
                new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build());
    }

    @AfterClass
    public static void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    public void testGetAllElectronicOrdersContainingValueOrderedBy_hqlParsesSuccessfully() {
        try (Session session = sessionFactory.openSession()) {
            String sql = "select eo from ElectronicOrder eo join eo.patient patient join patient.person person  where"
                    + " lower(eo.data) like concat('%', lower(:searchValue), '%') or"
                    + " lower(person.firstName) like concat('%', lower(:searchValue), '%') or"
                    + " lower(person.lastName) like concat('%', lower(:searchValue), '%') or patient.id in"
                    + " (SELECT identity.patientId FROM PatientIdentity identity WHERE"
                    + " identity.identityData like concat('%', :searchValue, '%')) or patient.nationalId"
                    + " like concat('%', :searchValue, '%') or lower(concat(person.firstName, ' ',"
                    + " person.lastName)) like concat('%', lower(:searchValue), '%') order by eo.statusId asc";

            assertNotNull(session.createQuery(sql, ElectronicOrder.class));
        }
    }

    @Test
    public void testGetAllElectronicOrdersMatchingAnyValue_hqlParsesSuccessfully() {
        try (Session session = sessionFactory.openSession()) {
            String hql = "select eo from ElectronicOrder eo join eo.patient patient join patient.person person where"
                    + " lower(eo.externalId) in (:identifierValues) or lower(person.firstName) ="
                    + " lower(:patientValue) or lower(person.lastName) = lower(:patientValue) or patient.id"
                    + " in (SELECT identity.patientId FROM PatientIdentity identity WHERE"
                    + " lower(identity.identityData) = lower(:patientValue)) or lower(patient.nationalId) ="
                    + " lower(:patientValue) or lower(concat(person.firstName, ' ', person.lastName)) ="
                    + " lower(:patientValue) order by eo.statusId asc";

            assertNotNull(session.createQuery(hql, ElectronicOrder.class));
        }
    }

    @Test
    public void testGetElectronicOrdersContainingValueExludedByOrderedBy_hqlParsesSuccessfully() {
        try (Session session = sessionFactory.openSession()) {
            String sql = "select eo from ElectronicOrder eo join eo.patient patient join patient.person person  where"
                    + " lower(eo.data) like concat('%', lower(:searchValue), '%') or"
                    + " lower(person.firstName) like concat('%', lower(:searchValue), '%') or"
                    + " lower(person.lastName) like concat('%', lower(:searchValue), '%') or"
                    + " lower(concat(person.firstName, ' ', person.lastName)) like concat('%',"
                    + " lower(:searchValue), '%')or patient.id in (SELECT identity.patientId FROM"
                    + " PatientIdentity identity WHERE identity.identityData like concat('%', :searchValue,"
                    + " '%')) or patient.nationalId like concat('%', :searchValue, '%') and eo.statusId not"
                    + " in (:excludedStatuses) order by eo.statusId asc";

            assertNotNull(session.createQuery(sql, ElectronicOrder.class));
        }
    }

    @Test
    public void testGetAllElectronicOrdersContainingValuesOrderedBy_hqlParsesSuccessfully() {
        try (Session session = sessionFactory.openSession()) {
            String sql = "select eo from ElectronicOrder eo join eo.patient patient join patient.person person "
                    + "where lower(eo.data) like concat('%', lower(:accessionNumber), '%') "
                    + "and lower(person.lastName) like concat('%', lower(:patientLastName), '%') "
                    + "and lower(person.firstName) like concat('%', lower(:patientFirstName), '%') "
                    + "and lower(patient.gender) = lower(:gender) order by eo.statusId asc";

            assertNotNull(session.createQuery(sql, ElectronicOrder.class));
        }
    }
}
