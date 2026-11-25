package org.openelisglobal.reports.tat.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.stat.Statistics;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.reports.tat.bean.TATCalculationMode;
import org.openelisglobal.reports.tat.bean.TATResult;
import org.openelisglobal.reports.tat.bean.TATSegment;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.service.PatientManagementUpdate;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TATReportOrderingSiteIntegrationTest extends BaseWebContextSensitiveTest {

    private static final LocalDate DAY = LocalDate.of(2099, 3, 10);

    @Autowired
    private TATReportService tatReportService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private SamplePatientEntryService samplePatientEntryService;

    @Test
    public void eachRowCarriesItsOwnSamplesOrderingSite() {
        String run = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        long north = organization("North clinic " + run);
        long south = organization("South clinic " + run);
        long east = organization("East clinic " + run);
        sample("N" + run, north);
        sample("S" + run, south);
        sample("O" + run);
        sample("D" + run, north, east);

        Map<String, String> sites = new HashMap<>();
        allResults().stream().filter(r -> r.getLabNumber().endsWith(run))
                .forEach(r -> sites.put(r.getLabNumber(), r.getOrderingSite()));

        assertEquals(Set.of("N" + run, "S" + run, "O" + run, "D" + run), sites.keySet());
        assertEquals("North clinic " + run, sites.get("N" + run));
        assertEquals("South clinic " + run, sites.get("S" + run));
        assertNull(sites.get("O" + run));
        assertEquals("East clinic " + run, sites.get("D" + run));
    }

    @Test
    public void anAddOrderRowShowsTheReferringSiteNotItsDepartment() {
        String run = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        addOrder("R" + run, organization("Referring clinic " + run), organization("A department " + run));

        Map<String, String> sites = new HashMap<>();
        allResults().stream().filter(r -> r.getLabNumber().endsWith(run))
                .forEach(r -> sites.put(r.getLabNumber(), r.getOrderingSite()));

        assertEquals(Set.of("R" + run), sites.keySet());
        assertEquals("Referring clinic " + run, sites.get("R" + run));
    }

    @Test
    public void filteringByTheReferringSiteKeepsAnAddOrderRow() {
        String run = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        long site = organization("Referring clinic " + run);
        addOrder("R" + run, site, organization("A department " + run));

        List<String> labNumbers = tatReportService.getAllResults(DAY, DAY, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, String.valueOf(site), false).stream()
                .map(TATResult::getLabNumber).toList();

        assertEquals(List.of("R" + run), labNumbers);
    }

    @Test
    public void moreRowsRunNoMoreQueries() {
        String run = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        long site = organization("Clinic " + run);
        sample("A" + run, site);
        sample("B" + run, site);
        long twoRows = queriesForOneDay();
        for (int i = 0; i < 6; i++) {
            sample("C" + i + run, site);
        }
        assertEquals(twoRows, queriesForOneDay());
    }

    @Test
    public void reportRowsDoNotStayInTheSession() {
        String run = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        sample("A" + run, organization("Clinic " + run));
        sample("B" + run);

        List<String> labNumbers = allResults().stream().map(TATResult::getLabNumber).filter(n -> n.endsWith(run))
                .sorted().toList();

        assertEquals(List.of("A" + run, "B" + run), labNumbers);
        assertEquals(List.of(), rowEntitiesInSession());
    }

    private List<String> rowEntitiesInSession() {
        Set<String> rowEntities = Set.of(Analysis.class.getName(), Sample.class.getName(), SampleItem.class.getName());
        return entityManager.unwrap(Session.class).getStatistics().getEntityKeys().stream()
                .map(key -> ((EntityKey) key).getEntityName()).filter(rowEntities::contains).toList();
    }

    private long queriesForOneDay() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean wasEnabled = statistics.isStatisticsEnabled();
        statistics.clear();
        statistics.setStatisticsEnabled(true);
        try {
            allResults();
            return statistics.getQueryExecutionCount();
        } finally {
            statistics.setStatisticsEnabled(wasEnabled);
        }
    }

    private List<TATResult> allResults() {
        return tatReportService.getAllResults(DAY, DAY, TATSegment.RECEIPT_TO_VALIDATION, TATCalculationMode.CALENDAR,
                null, null, null, null, null, null, false);
    }

    // The calls SamplePatientEntryRestController makes to save a new order.
    private void addOrder(String accession, long site, long department) {
        fixtures.ensureRequiredWorkflowStatuses();
        // Other classes' datasets drop the dept type Add Order links.
        String deptType = TableIdService.getInstance().REFERRING_ORG_DEPARTMENT_TYPE_ID;
        jdbcTemplate.update(
                "INSERT INTO clinlims.organization_type (id, short_name, description)"
                        + " SELECT CAST(? AS numeric), 'dept', 'Organisation department'"
                        + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.organization_type WHERE id = CAST(? AS numeric))",
                deptType, deptType);
        String userId = fixtures.defaultUserId();

        SampleOrderItem order = new SampleOrderItem();
        order.setLabNo(accession);
        order.setReferringSiteId(String.valueOf(site));
        order.setReferringSiteDepartmentId(String.valueOf(department));

        SamplePatientUpdateData updateData = new SamplePatientUpdateData(userId);
        updateData.initializeRequester(order);
        updateData.setAccessionNumber(accession);
        updateData.initSampleData("<samples><sample sampleID='1' typeId='"
                + fixtures.getOrCreateActiveSampleType().getId() + "' tests='" + fixtures.createCatalogTest().getId()
                + "' testSectionMap='' testSampleTypeMap='' panels='' date='' time='' initialConditionIds=''/>"
                + "</samples>", DateUtil.formatDateAsText(Date.valueOf(DAY)) + " 09:00", false, order);

        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK(fixtures.createPatient("TAT").getId());
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setPatientProperties(patientInfo);
        samplePatientEntryService.persistData(updateData, SpringContext.getBean(PatientManagementUpdate.class),
                patientInfo, form, new MockHttpServletRequest());
        entityManager.flush();
    }

    private long organization(String name) {
        return jdbcTemplate.queryForObject("INSERT INTO clinlims.organization (id, name)"
                + " VALUES (nextval('clinlims.organization_seq'), ?) RETURNING id", Long.class, name);
    }

    private void sample(String accession, long... organizationIds) {
        long sampleId = jdbcTemplate.queryForObject(
                "INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date)"
                        + " VALUES (nextval('clinlims.sample_seq'), ?, ?, ?) RETURNING id",
                Long.class, accession, DAY.atTime(8, 0), DAY.atTime(9, 0));
        long itemId = jdbcTemplate
                .queryForObject(
                        "INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id)"
                                + " VALUES (nextval('clinlims.sample_item_seq'), ?, 1,"
                                + " (SELECT min(id) FROM clinlims.status_of_sample)) RETURNING id",
                        Long.class, sampleId);
        jdbcTemplate.update("INSERT INTO clinlims.analysis"
                + " (id, sampitem_id, analysis_type, status_id, started_date, completed_date, released_date)"
                + " VALUES (nextval('clinlims.analysis_seq'), ?, 'MANUAL', (SELECT min(id) FROM clinlims.status_of_sample"
                + " WHERE status_type = 'ANALYSIS' AND name <> 'Test Canceled'), ?, ?, ?)", itemId, DAY.atTime(10, 0),
                DAY.atTime(11, 0), DAY.atTime(12, 0));
        for (long organizationId : organizationIds) {
            jdbcTemplate.update("INSERT INTO clinlims.sample_organization (id, samp_id, org_id)"
                    + " VALUES (nextval('clinlims.sample_org_seq'), ?, ?)", sampleId, organizationId);
        }
    }
}
