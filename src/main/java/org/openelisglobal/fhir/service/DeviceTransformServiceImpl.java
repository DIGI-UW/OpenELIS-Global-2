package org.openelisglobal.fhir.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Device.DeviceDeviceNameComponent;
import org.hl7.fhir.r4.model.Device.DeviceNameType;
import org.hl7.fhir.r4.model.Device.FHIRDeviceStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeviceTransformServiceImpl implements DeviceTransformService {

    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private AnalyzerService analyzerService;
    @Autowired
    private FhirCommonTransformService common;

    @Override
    public Analyzer transformDeviceToAnalyzer(Device device) {
        Analyzer analyzer = new Analyzer();

        if (device.hasId()) {

            String fhirUuid = device.getIdElement().getIdPart();

            try {

                UUID uuid = UUID.fromString(fhirUuid);

                List<Analyzer> analyzers = analyzerService.getAllMatching("fhirUuid", uuid);

                if (!analyzers.isEmpty()) {
                    analyzer = analyzerService.getWithBinding(analyzers.get(0).getId()).orElse(analyzers.get(0));
                } else {
                    analyzer.setFhirUuid(uuid);
                }

            } catch (IllegalArgumentException e) {

                throw new IllegalArgumentException("Invalid Device.id UUID: " + fhirUuid, e);
            }

        } else {

            analyzer.setFhirUuid(UUID.randomUUID());
        }

        if (device.hasStatus()) {

            switch (device.getStatus()) {

            case ACTIVE:
                analyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
                break;

            case ENTEREDINERROR:
                analyzer.setStatus(Analyzer.AnalyzerStatus.ERROR_PENDING);
                break;

            case INACTIVE:
                analyzer.setStatus(Analyzer.AnalyzerStatus.INACTIVE);
                break;

            case UNKNOWN:
                analyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
                break;

            default:
                analyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
            }
        }

        if (device.hasDeviceName()) {

            DeviceDeviceNameComponent nameComponent = device.getDeviceNameFirstRep();

            if (nameComponent.hasName()) {
                analyzer.setName(nameComponent.getName());
            }
        }

        for (Identifier identifier : device.getIdentifier()) {
            if (identifier.hasSystem() && identifier.hasValue()
                    && identifier.getSystem().endsWith("/analyzer_bridge_connection")) {
                analyzer.setBridgeConnectionId(identifier.getValue());
            }
        }

        Extension activationExtension = device
                .getExtensionByUrl("http://openelis.org/fhir/StructureDefinition/analyzer-last-activated");

        if (activationExtension != null && activationExtension.getValue() instanceof DateTimeType) {

            analyzer.setLastActivatedDate(((DateTimeType) activationExtension.getValue()).getValue());
        }

        Extension testUnitsExtension = device
                .getExtensionByUrl("http://openelis.org/fhir/StructureDefinition/analyzer-test-units");

        if (testUnitsExtension != null) {

            List<String> testUnitIds = new ArrayList<>();

            for (Extension ext : testUnitsExtension.getExtension()) {

                if ("testUnitId".equals(ext.getUrl()) && ext.getValue() instanceof StringType) {

                    testUnitIds.add(((StringType) ext.getValue()).getValue());
                }
            }

            analyzer.setTestUnitIds(testUnitIds);
        }

        return analyzer;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The id falls back to the row id rather than {@code ensureFhirUuid()}, which
     * invents a UUID when the column is null. This is a read-only path, so that
     * invented value was never persisted: every request handed the client a
     * different id for the same analyzer, and none of them resolved.
     */
    @Override
    public Device transformAnalyzerToDevice(Analyzer analyzer) {
        Device device = new Device();

        String fhirUuid = analyzer.getFhirUuid() == null ? analyzer.getId() : analyzer.getFhirUuidAsString();
        device.setId(fhirUuid);
        device.getMeta().setLastUpdated(analyzer.getLastupdated());
        if (analyzer.getStatus() != null) {

            switch (analyzer.getStatus()) {
            case VALIDATION:
            case SETUP:
            case ACTIVE:
                device.setStatus(FHIRDeviceStatus.ACTIVE);
                break;

            case ERROR_PENDING:
                device.setStatus(FHIRDeviceStatus.ENTEREDINERROR);
                break;

            case INACTIVE:
                device.setStatus(FHIRDeviceStatus.INACTIVE);
                break;

            case OFFLINE:
                device.setStatus(FHIRDeviceStatus.UNKNOWN);
                break;

            default:
                device.setStatus(FHIRDeviceStatus.UNKNOWN);
            }
        }

        device.addIdentifier(common.createIdentifier(fhirConfig.getOeFhirSystem() + "/analyzer_uuid", fhirUuid));

        if (!GenericValidator.isBlankOrNull(analyzer.getBridgeConnectionId())) {
            device.addIdentifier(common.createIdentifier(fhirConfig.getOeFhirSystem() + "/analyzer_bridge_connection",
                    analyzer.getBridgeConnectionId()));
        }

        if (!GenericValidator.isBlankOrNull(analyzer.getName())) {

            device.addDeviceName(new DeviceDeviceNameComponent().setName(analyzer.getName())
                    .setType(DeviceNameType.USERFRIENDLYNAME));
        }

        if (analyzer.getPinnedProfileBinding() != null
                && !GenericValidator.isBlankOrNull(analyzer.getPinnedProfileBinding().getProfileId())) {
            device.setType(new CodeableConcept().setText(analyzer.getPinnedProfileBinding().getProfileId()));
        }

        if (analyzer.getLastActivatedDate() != null) {

            device.addExtension(new Extension("http://openelis.org/fhir/StructureDefinition/analyzer-last-activated",
                    new DateTimeType(analyzer.getLastActivatedDate())));
        }

        if (analyzer.getTestUnitIds() != null && !analyzer.getTestUnitIds().isEmpty()) {

            Extension testUnitsExtension = new Extension(
                    "http://openelis.org/fhir/StructureDefinition/analyzer-test-units");

            analyzer.getTestUnitIds().stream().filter(id -> !GenericValidator.isBlankOrNull(id))
                    .forEach(id -> testUnitsExtension.addExtension(new Extension("testUnitId", new StringType(id))));

            device.addExtension(testUnitsExtension);
        }

        Identifier facilityIdentifier = common.createFacilityIdentifier();

        if (facilityIdentifier != null) {

            device.setOwner(new Reference().setIdentifier(facilityIdentifier));
        }

        device.getMeta().addProfile("http://openelis.org/fhir/StructureDefinition/openelis-analyzer-device");

        if (analyzer.getStatus() != null) {

            device.addExtension(
                    new Extension("http://openelis.org/fhir/StructureDefinition/analyzer-operational-status",
                            new CodeType(analyzer.getStatus().name())));
        }

        return device;
    }
}
