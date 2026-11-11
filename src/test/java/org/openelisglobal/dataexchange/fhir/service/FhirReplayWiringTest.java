package org.openelisglobal.dataexchange.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Task;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.dataexchange.fhir.controller.FhirReplayRestController;
import org.openelisglobal.dataexchange.fhir.form.FhirReplayRequest;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;
import org.openelisglobal.fhir.service.TaskTransformService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;

public class FhirReplayWiringTest {

    @Test
    public void controllerResolvesTheWrappedTransformAfterCircularDependencyInitialization() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(TestConfig.class, FhirReplayRestController.class, ReferralDependency.class,
                    CircularTransformService.class);
            // Keep real transform logic and Spring async wiring; isolate its external
            // collaborators.
            for (Field field : FhirTransformServiceImpl.class.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    context.getBeanFactory().registerSingleton(field.getName(), mock(field.getType()));
                }
            }
            context.refresh();

            FhirTransformService transform = context.getBean(FhirTransformService.class);
            assertTrue("The controller must resolve Spring's async proxy", AopUtils.isAopProxy(transform));
            assertSame(transform, context.getBean(ReferralDependency.class).transform);

            Sample sample = new Sample();
            sample.setId("1154");
            sample.setFhirUuid(UUID.fromString("47900000-0000-4000-8000-000000000001"));
            when(context.getBean(SampleService.class).get("1154")).thenReturn(sample);
            Task task = new Task();
            task.setId(sample.getFhirUuid().toString());
            when(context.getBean(TaskTransformService.class).transformToTask(sample)).thenReturn(task);
            Bundle response = new Bundle().setType(Bundle.BundleType.TRANSACTIONRESPONSE);
            response.addEntry().getResponse().setStatus("200 OK");
            FhirPersistanceService store = context.getBean(FhirPersistanceService.class);
            ArgumentCaptor<FhirOperations> sent = ArgumentCaptor.forClass(FhirOperations.class);
            when(store.createUpdateFhirResourcesInFhirStore(sent.capture())).thenReturn(response);

            FhirReplayRequest request = new FhirReplayRequest();
            request.setSampleIds(List.of("1154"));
            assertEquals(HttpStatus.OK,
                    context.getBean(FhirReplayRestController.class).replay(request).getStatusCode());
            verify(store).createUpdateFhirResourcesInFhirStore(sent.getValue());
            assertEquals(Set.of("Task/47900000-0000-4000-8000-000000000001"), sent.getValue().updateResources.keySet());
        }
    }

    // Reproduce the existing referral/transform cycle without loading the whole
    // database-backed app.
    public static class ReferralDependency {
        @Autowired
        FhirTransformService transform;
    }

    public static class CircularTransformService extends FhirTransformServiceImpl {
        @Autowired
        ReferralDependency referral;
    }

    @Configuration
    @EnableAsync
    // AppTestConfig excludes nested TestConfig classes from its shared component
    // scan.
    static class TestConfig {
        @Bean
        Executor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }
}
