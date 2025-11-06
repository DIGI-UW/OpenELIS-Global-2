package org.openelisglobal.config.task;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Narrative.NarrativeStatus;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelComponent;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType;
import org.hl7.fhir.r4.model.Subscription.SubscriptionStatus;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.itech.fhir.dataexport.core.model.DataExportTask;
import org.itech.fhir.dataexport.core.service.DataExportTaskService;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RegisterFhirHooksTask {

    @Value("${org.openelisglobal.fhir.subscriber}")
    private Optional<String> fhirSubscriber;

    @Value("${org.openelisglobal.fhir.subscriber.allowHTTP:false}")
    private Boolean allowHTTP;

    @Value("${org.openelisglobal.fhir.subscriber.resources}")
    private String[] fhirSubscriptionResources;

    @Value("${org.openelisglobal.fhirstore.uri}")
    private String localFhirStorePath;

    @Value("${org.openelisglobal.fhir.subscriber.backup.interval:5}")
    private Integer backupInterval;

    @Value("${org.openelisglobal.fhir.subscriber.backup.timeout:60}")
    private Integer backupTimeout;

    private static String fhirSubscriptionIdPrefix = "consolidatedServerSubscription";

    @Autowired
    FhirContext fhirContext;
    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private DataExportTaskService dataExportTaskService;

    @PostConstruct
    public void startTask() {
        if (!fhirSubscriber.isPresent() || GenericValidator.isBlankOrNull(fhirSubscriber.get())) {
            return;
        }
        if (!allowHTTP && fhirSubscriber.get().startsWith("http://")) {
            fhirSubscriber = Optional.of("https://" + fhirSubscriber.get().substring("http://".length()));
        }

        if (!(allowHTTP && fhirSubscriber.get().startsWith("http://"))
                && !fhirSubscriber.get().startsWith("https://")) {
            fhirSubscriber = Optional.of("https://" + fhirSubscriber.get());
        }
        if (GenericValidator.isBlankOrNull(localFhirStorePath)) {
            return;
        }

        IGenericClient fhirClient = fhirUtil.getFhirClient(localFhirStorePath);

        removeOldSubscription();

        Bundle subscriptionBundle = new Bundle();
        subscriptionBundle.setType(BundleType.TRANSACTION);

        for (String fhirSubscriptionResource : fhirSubscriptionResources) {
            ResourceType resourceType = ResourceType.fromCode(fhirSubscriptionResource);
            Subscription subscription = createSubscriptionForResource(resourceType);
            BundleEntryComponent bundleEntry = new BundleEntryComponent();
            bundleEntry.setResource(subscription);
            bundleEntry.setRequest(new BundleEntryRequestComponent().setMethod(HTTPVerb.PUT).setUrl(
                    ResourceType.Subscription.name() + "/" + createSubscriptionIdForResourceType(resourceType)));

            subscriptionBundle.addEntry(bundleEntry);
        }
        try {
            Bundle returnedBundle = fhirClient.transaction().withBundle(subscriptionBundle).encodedJson().execute();
            LogEvent.logDebug(this.getClass().getSimpleName(), "startTask", "subscription bundle returned:\n"
                    + fhirContext.newJsonParser().encodeResourceToString(returnedBundle));
        } catch (UnprocessableEntityException | DataFormatException e) {
            LogEvent.logError("error while communicating subscription bundle to " + localFhirStorePath + " for "
                    + fhirSubscriber.get(), e);
        }

        DataExportTask dataExportTask = dataExportTaskService.getDAO().findByEndpoint(fhirSubscriber.get())
                .orElse(new DataExportTask());
        Map<String, String> headers = new HashMap<>();
        headers.put("Server-Name", ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName));
        headers.put("Server-Code", ConfigurationProperties.getInstance().getPropertyValue(Property.SiteCode));

        dataExportTask.setFhirResources(Arrays.asList(fhirSubscriptionResources).stream().map(ResourceType::fromCode)
                .collect(Collectors.toList()));
        dataExportTask.setHeaders(headers);
        dataExportTask.setMaxDataExportInterval(backupInterval); // minutes
        dataExportTask.setDataRequestAttemptTimeout(backupTimeout); // seconds // currently unused
        dataExportTask.setEndpoint(fhirSubscriber.get());
        dataExportTaskService.getDAO().save(dataExportTask);
    }

    private void removeOldSubscription() {
        IGenericClient fhirClient = fhirUtil.getFhirClient(localFhirStorePath);

        Bundle deleteTransactionBundle = new Bundle();
        deleteTransactionBundle.setType(BundleType.TRANSACTION);
        for (ResourceType resourceType : ResourceType.values()) {
            Bundle responseBundle = (Bundle) fhirClient.search().forResource(Subscription.class)
                    .where(Subscription.RES_ID.exactly().code(createSubscriptionIdForResourceType(resourceType)))
                    .execute();
            if (responseBundle.hasEntry()) {
                BundleEntryComponent bundleEntry = new BundleEntryComponent();
                bundleEntry.setRequest(new BundleEntryRequestComponent().setMethod(HTTPVerb.DELETE).setUrl(
                        ResourceType.Subscription.name() + "/" + createSubscriptionIdForResourceType(resourceType)));

                deleteTransactionBundle.addEntry(bundleEntry);
            }
        }
        Bundle returnedBundle = fhirClient.transaction().withBundle(deleteTransactionBundle).encodedJson().execute();
        LogEvent.logTrace(this.getClass().getSimpleName(), "removeOldSubscription",
                "delete old bundle returned:\n" + fhirContext.newJsonParser().encodeResourceToString(returnedBundle));
    }

    private String createSubscriptionIdForResourceType(ResourceType resourceType) {
        return fhirSubscriptionIdPrefix + resourceType.toString();
    }

    private Subscription createSubscriptionForResource(ResourceType resourceType) {
        Subscription subscription = new Subscription();
        subscription.setText(new Narrative().setStatus(NarrativeStatus.GENERATED).setDiv(new XhtmlNode(NodeType.Text)));
        subscription.setStatus(SubscriptionStatus.REQUESTED);
        subscription
                .setReason("bulk subscription to detect any Creates or Updates to resources of type " + resourceType);

        SubscriptionChannelComponent channel = new SubscriptionChannelComponent();
        channel.setType(SubscriptionChannelType.RESTHOOK).setEndpoint(fhirSubscriber.get());
        channel.addHeader("Server-Name: " + ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName));
        channel.addHeader("Server-Code: " + ConfigurationProperties.getInstance().getPropertyValue(Property.SiteCode));
        channel.setPayload("application/fhir+json");
        subscription.setChannel(channel);

        subscription.setCriteria(createCriteriaString(resourceType));
        return subscription;
    }

    private String createCriteriaString(ResourceType resourceType) {
        return resourceType.name() + "?";
    }
}
