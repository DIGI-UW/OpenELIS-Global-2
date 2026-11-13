package org.openelisglobal.containertype.service;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.containertype.form.ContainerTypeForm;
import org.openelisglobal.containertype.form.ContainerTypeView;
import org.openelisglobal.containertype.form.TestContainersForm;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-1611 P2: two administrators saving the same test's containers at once
 * both succeed, one after the other, and the test keeps one complete list.
 */
public class TestContainerConcurrencyIntegrationTest extends BaseWebContextSensitiveTest {

    private static final int ADMINS = 6;
    private static final int ROUNDS = 3;

    @Autowired
    private TestContainerService testContainerService;
    @Autowired
    private ContainerTypeService containerTypeService;
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;
    private TypeOfSample sampleType;
    private String testId;
    private ContainerTypeView preferred;
    private ContainerTypeView alternate;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        sampleType = fixtures.createTypeOfSample();
        testId = fixtures.createCatalogTest().getId();
        jdbc.update(
                "INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id) VALUES"
                        + " (nextval('clinlims.sample_type_test_seq'), ?::numeric, ?::numeric)",
                sampleType.getId(), testId);
        typeOfSampleService.clearCache();
        preferred = container("QA1611-RACE-A");
        alternate = container("QA1611-RACE-B");
    }

    @After
    public void deleteCommittedRows() {
        jdbc.update("DELETE FROM clinlims.test_expected_container WHERE test_id = ?::numeric", testId);
        jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id = ?::numeric", testId);
        for (ContainerTypeView container : List.of(preferred, alternate)) {
            jdbc.update("DELETE FROM clinlims.container_type_sample_type WHERE container_type_id = ?", container.id());
            jdbc.update("DELETE FROM clinlims.container_type_domain WHERE container_type_id = ?", container.id());
            jdbc.update("DELETE FROM clinlims.container_type WHERE id = ?", container.id());
        }
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?::numeric", testId);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id = ?::numeric", sampleType.getId());
        typeOfSampleService.clearCache();
    }

    @Test
    public void simultaneousSavesOfOneTestAllSucceedAndLeaveOneList() throws Exception {
        TestContainersForm form = new TestContainersForm(
                List.of(new TestContainersForm.Row(preferred.id(), "PREFERRED", 1, null, null, null),
                        new TestContainersForm.Row(alternate.id(), "ALTERNATE", 1, null, null, null)),
                false);
        List<String> failures = new ArrayList<>();
        for (int round = 0; round < ROUNDS; round++) {
            ExecutorService pool = Executors.newFixedThreadPool(ADMINS);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Throwable>> outcomes = new ArrayList<>();
            for (int admin = 0; admin < ADMINS; admin++) {
                outcomes.add(pool.submit(() -> {
                    start.await();
                    try {
                        testContainerService.save(testId, form, "1");
                        return null;
                    } catch (Throwable t) {
                        return t;
                    }
                }));
            }
            start.countDown();
            pool.shutdown();
            for (Future<Throwable> outcome : outcomes) {
                Throwable failure = outcome.get();
                if (failure != null) {
                    failures.add(failure.toString());
                }
            }
        }

        assertEquals("no save should fail: " + failures, 0, failures.size());
        assertEquals(List.of("QA1611-RACE-A", "QA1611-RACE-B"),
                testContainerService.forTest(testId).orElseThrow().containers().stream().map(r -> r.code()).toList());
    }

    private ContainerTypeView container(String code) {
        return containerTypeService.save(null, new ContainerTypeForm(code, code, null, null, null, null, null, null,
                null, null, new ArrayList<>(List.of("CLINICAL")), List.of(sampleType.getId())), "1");
    }
}
