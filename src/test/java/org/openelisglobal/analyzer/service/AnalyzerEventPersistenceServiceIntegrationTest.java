package org.openelisglobal.analyzer.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.dao.AnalyzerEventDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class AnalyzerEventPersistenceServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalyzerEventPersistenceService persistenceService;

    @Autowired
    private AnalyzerEventDAO eventDAO;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String externalEventId;

    @After
    public void removeEvent() {
        if (externalEventId != null) {
            new TransactionTemplate(transactionManager).executeWithoutResult(
                    status -> eventDAO.getByExternalEventId(externalEventId).ifPresent(eventDAO::delete));
        }
    }

    @Test
    public void concurrentDeliveriesHaveExactlyOneProcessingOwner() throws Exception {
        externalEventId = "integration-" + UUID.randomUUID();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<AnalyzerEventRegistration>> futures = List.of(
                    executor.submit(() -> registerAtStart(ready, start)),
                    executor.submit(() -> registerAtStart(ready, start)));

            assertTrue("Both deliveries must be ready", ready.await(10, SECONDS));
            start.countDown();
            AnalyzerEventRegistration first = futures.get(0).get(30, SECONDS);
            AnalyzerEventRegistration second = futures.get(1).get(30, SECONDS);

            assertEquals(1, List.of(first, second).stream().filter(AnalyzerEventRegistration::created).count());
            assertEquals(first.event().getId(), second.event().getId());
            assertEquals(externalEventId,
                    eventDAO.getByExternalEventId(externalEventId).orElseThrow().getExternalEventId());
        } finally {
            executor.shutdownNow();
            assertTrue("Concurrent delivery workers must terminate", executor.awaitTermination(10, SECONDS));
        }
    }

    private AnalyzerEventRegistration registerAtStart(CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        assertTrue("Concurrent delivery start was not released", start.await(10, SECONDS));
        AnalyzerEvent event = new AnalyzerEvent();
        event.setExternalEventId(externalEventId);
        event.setEventType("AST_RESULT_AVAILABLE");
        event.setPayload("{}");
        return persistenceService.createIfAbsent(event);
    }
}
