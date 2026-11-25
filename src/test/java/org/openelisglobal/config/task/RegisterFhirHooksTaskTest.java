package org.openelisglobal.config.task;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Subscription;
import org.itech.fhir.dataexport.core.model.DataExportTask;
import org.itech.fhir.dataexport.core.service.DataExportTaskService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

public class RegisterFhirHooksTaskTest {
    private final RegisterFhirHooksTask task = new RegisterFhirHooksTask();
    private final FhirUtil fhir = mock(FhirUtil.class);
    private final IGenericClient client = mock(IGenericClient.class, RETURNS_DEEP_STUBS);
    private final DataExportTaskService exports = mock(DataExportTaskService.class);
    private final org.itech.fhir.dataexport.core.dao.DataExportTaskDAO tasks = mock(
            org.itech.fhir.dataexport.core.dao.DataExportTaskDAO.class);
    private Object previousFactory;

    @Before
    public void setUp() {
        when(exports.getDAO()).thenReturn(tasks);
        previousFactory = ReflectionTestUtils.getField(SpringContext.class, "factory");
        AutowireCapableBeanFactory factory = mock(AutowireCapableBeanFactory.class);
        when(factory.getBean(DefaultConfigurationProperties.class))
                .thenReturn(mock(DefaultConfigurationProperties.class));
        ReflectionTestUtils.setField(SpringContext.class, "factory", factory);
        ReflectionTestUtils.setField(task, "fhirUtil", fhir);
        ReflectionTestUtils.setField(task, "fhirContext", FhirContext.forR4());
        ReflectionTestUtils.setField(task, "dataExportTaskService", exports);
        ReflectionTestUtils.setField(task, "allowHTTP", false);
        ReflectionTestUtils.setField(task, "localFhirStorePath", "http://local-fhir:8080/fhir/");
        ReflectionTestUtils.setField(task, "fhirSubscriptionResources", new String[] { "Patient", "Observation" });
        ReflectionTestUtils.setField(task, "backupInterval", 5);
        ReflectionTestUtils.setField(task, "backupTimeout", 60);
        when(fhir.getFhirClient(anyString())).thenReturn(client);
        var query = client.search().forResource(Subscription.class)
                .where(any(ca.uhn.fhir.rest.gclient.ICriterion.class));
        doReturn(new Bundle()).when(query).execute();
        ca.uhn.fhir.rest.gclient.ITransactionTyped<Bundle> transaction = mock(
                ca.uhn.fhir.rest.gclient.ITransactionTyped.class, RETURNS_SELF);
        when(client.transaction().withBundle(any(Bundle.class))).thenReturn(transaction);
        doReturn(new Bundle()).when(transaction).execute();
        when(exports.getDAO().findByEndpoint(anyString())).thenReturn(Optional.empty());
    }

    @After
    public void restoreFactory() {
        ReflectionTestUtils.setField(SpringContext.class, "factory", previousFactory);
    }

    @Test
    public void httpsRegistersTheSameDestinationForHooksAndScheduledExports() {
        assertDestination("https://partner.example/fhir/");
    }

    @Test
    public void explicitTestOverrideKeepsTheSameHttpDestination() {
        ReflectionTestUtils.setField(task, "allowHTTP", true);
        assertDestination("http://127.0.0.1:8090/fhir/");
    }

    @Test
    public void unsafeOrAmbiguousUrlsAreRejectedBeforeAnyStoreOrJobChanges() {
        for (String endpoint : new String[] { "http://partner.example/fhir/", "HTTP://partner.example/fhir/",
                "partner.example/fhir/", "ftp://partner.example/fhir/", "https:///fhir/",
                "https://user:password@partner.example/fhir/", "https://partner.example/fhir/#fragment",
                "https://partner.example/fhir/?other=destination", "https://bad host/fhir/" }) {
            ReflectionTestUtils.setField(task, "fhirSubscriber", Optional.of(endpoint));
            assertThrows(endpoint, IllegalArgumentException.class, task::startTask);
        }
        verifyZeroInteractions(fhir);
        verify(exports.getDAO(), never()).save(any());
    }

    @Test
    public void blankSubscriberDoesNotEnableExports() {
        ReflectionTestUtils.setField(task, "fhirSubscriber", Optional.of(""));
        task.startTask();
        verifyZeroInteractions(fhir);
        verify(exports.getDAO(), never()).save(any());
    }

    private void assertDestination(String endpoint) {
        ReflectionTestUtils.setField(task, "fhirSubscriber", Optional.of(endpoint));
        task.startTask();
        ArgumentCaptor<Bundle> bundles = ArgumentCaptor.forClass(Bundle.class);
        verify(client.transaction(), times(2)).withBundle(bundles.capture());
        Bundle registration = bundles.getAllValues().get(1);
        assertEquals(2, registration.getEntry().size());
        registration.getEntry().forEach(
                entry -> assertEquals(endpoint, ((Subscription) entry.getResource()).getChannel().getEndpoint()));
        ArgumentCaptor<DataExportTask> job = ArgumentCaptor.forClass(DataExportTask.class);
        verify(exports.getDAO()).save(job.capture());
        assertEquals(endpoint, job.getValue().getEndpoint());
        assertEquals(Optional.of(endpoint), ReflectionTestUtils.getField(task, "fhirSubscriber"));
    }
}
