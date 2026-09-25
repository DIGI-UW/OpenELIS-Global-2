package org.openelisglobal.fhir.service;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Device.DeviceDeviceNameComponent;
import org.hl7.fhir.r4.model.Device.DeviceNameType;
import org.hl7.fhir.r4.model.Device.FHIRDeviceStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.AnalyzerTypeService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.analyzer.valueholder.CommunicationMode;
import org.openelisglobal.analyzer.valueholder.ProtocolVersion;
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
    private AnalyzerTypeService analyzerTypeService;
    @Autowired
    private FhirCommonTransformService common;

    @Override
    public Analyzer transformDeviceToAnalyzer(Device device) {

        final String COMM_MODE_EXTENSION = "http://openelis.org/fhir/StructureDefinition/analyzer-communication-mode";

        final String PROTOCOL_EXTENSION = "http://openelis.org/fhir/StructureDefinition/analyzer-protocol-version";

        final String TRANSPORT_EXTENSION = "http://openelis.org/fhir/StructureDefinition/analyzer-transport-details";

        Analyzer analyzer = new Analyzer();

        if (device.hasId()) {

            String fhirUuid = device.getIdElement().getIdPart();

            try {

                UUID uuid = UUID.fromString(fhirUuid);

                List<Analyzer> analyzers = analyzerService.getAllMatching("fhirUuid", uuid);

                if (!analyzers.isEmpty()) {
                    analyzer = analyzers.get(0);
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
                analyzer.setStatus(Analyzer.AnalyzerStatus.PENDING_REGISTRATION);
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

        if (device.hasType() && device.getType().hasText()) {

            String typeText = device.getType().getText();

            analyzer.setType(typeText);

            AnalyzerType analyzerType = analyzerTypeService.getAnalyzerTypeByName(typeText);

            if (analyzerType != null) {
                analyzer.setAnalyzerType(analyzerType);
            }
        }

        if (device.hasSerialNumber()) {
            analyzer.setMachineId(device.getSerialNumber());
        }

        for (Identifier identifier : device.getIdentifier()) {

            if (identifier.hasSystem() && identifier.hasValue()) {

                String system = identifier.getSystem();

                if (system.endsWith("/analyzer_sourceId")) {

                    analyzer.setDiscoveredSourceId(identifier.getValue());
                }
            }
        }

        Extension communicationExtension = device.getExtensionByUrl(COMM_MODE_EXTENSION);

        if (communicationExtension != null && communicationExtension.getValue() instanceof CodeableConcept) {

            CodeableConcept concept = (CodeableConcept) communicationExtension.getValue();

            if (concept.hasCoding()) {

                String code = concept.getCodingFirstRep().getCode();
                CommunicationMode mode = CommunicationMode.fromValue(code);
                if (mode == null) {
                    throw new UnprocessableEntityException("Unknown analyzer communication mode: " + code);
                }
                analyzer.setCommunicationMode(mode);
            }
        }

        Extension protocolExtension = device.getExtensionByUrl(PROTOCOL_EXTENSION);

        if (protocolExtension != null && protocolExtension.getValue() instanceof CodeableConcept) {

            CodeableConcept concept = (CodeableConcept) protocolExtension.getValue();

            if (concept.hasCoding()) {

                String code = concept.getCodingFirstRep().getCode();
                ProtocolVersion version = ProtocolVersion.fromValue(code);
                if (version == null) {
                    throw new UnprocessableEntityException("Unknown analyzer protocol version: " + code);
                }
                analyzer.setProtocolVersion(version);
            }
        }

        Extension transportExtension = device.getExtensionByUrl(TRANSPORT_EXTENSION);

        if (transportExtension != null) {

            for (Extension ext : transportExtension.getExtension()) {

                String url = ext.getUrl();

                if (ext.getValue() instanceof StringType) {

                    String value = ((StringType) ext.getValue()).getValue();

                    switch (url) {

                    case "ipAddress":
                        analyzer.setIpAddress(value);
                        break;

                    case "importDirectory":
                        analyzer.setImportDirectory(value);
                        break;

                    case "filePattern":
                        analyzer.setFilePattern(value);
                        break;

                    case "fileFormat":
                        analyzer.setFileFormat(value);
                        break;

                    case "delimiter":
                        analyzer.setDelimiter(value);
                        break;
                    }
                }

                if (ext.getValue() instanceof IntegerType) {

                    Integer value = ((IntegerType) ext.getValue()).getValue();

                    switch (url) {

                    case "port":
                        analyzer.setPort(value);
                        break;

                    case "skipRows":
                        analyzer.setSkipRows(value);
                        break;
                    }
                }

                if (ext.getValue() instanceof BooleanType) {

                    Boolean value = ((BooleanType) ext.getValue()).getValue();

                    if ("hasHeader".equals(url)) {
                        analyzer.setHasHeader(value);
                    }
                }
            }
        }

        Extension locationExtension = device
                .getExtensionByUrl("http://openelis.org/fhir/StructureDefinition/analyzer-location");

        if (locationExtension != null && locationExtension.getValue() instanceof StringType) {

            analyzer.setLocation(((StringType) locationExtension.getValue()).getValue());
        }

        Extension identifierPatternExtension = device
                .getExtensionByUrl("http://openelis.org/fhir/StructureDefinition/analyzer-identifier-pattern");

        if (identifierPatternExtension != null && identifierPatternExtension.getValue() instanceof StringType) {

            analyzer.setIdentifierPattern(((StringType) identifierPatternExtension.getValue()).getValue());
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

        if (device.hasNote()) {

            analyzer.setDescription(device.getNoteFirstRep().getText());
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

        final String COMM_MODE_EXTENSION = "http://openelis.org/fhir/StructureDefinition/analyzer-communication-mode";

        final String COMM_MODE_CODESYSTEM = "http://openelis.org/fhir/CodeSystem/analyzer-communication-mode";

        final String PROTOCOL_EXTENSION = "http://openelis.org/fhir/StructureDefinition/analyzer-protocol-version";

        final String TRANSPORT_EXTENSION = "http://openelis.org/fhir/StructureDefinition/analyzer-transport-details";

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
            case DELETED:
                device.setStatus(FHIRDeviceStatus.INACTIVE);
                break;

            case PENDING_REGISTRATION:
                device.setStatus(FHIRDeviceStatus.UNKNOWN);
                break;

            default:
                device.setStatus(FHIRDeviceStatus.ACTIVE);
            }
        }

        device.addIdentifier(common.createIdentifier(fhirConfig.getOeFhirSystem() + "/analyzer_uuid", fhirUuid));

        if (!GenericValidator.isBlankOrNull(analyzer.getMachineId())) {

            device.addIdentifier(common.createIdentifier(fhirConfig.getOeFhirSystem() + "/analyzer_machineId",
                    analyzer.getMachineId()));

            device.setSerialNumber(analyzer.getMachineId());
        }

        if (!GenericValidator.isBlankOrNull(analyzer.getDiscoveredSourceId())) {

            device.addIdentifier(common.createIdentifier(fhirConfig.getOeFhirSystem() + "/analyzer_sourceId",
                    analyzer.getDiscoveredSourceId()));
        }

        if (!GenericValidator.isBlankOrNull(analyzer.getName())) {

            device.addDeviceName(new DeviceDeviceNameComponent().setName(analyzer.getName())
                    .setType(DeviceNameType.USERFRIENDLYNAME));
        }

        if (!GenericValidator.isBlankOrNull(analyzer.getType())) {

            device.setType(new CodeableConcept().setText(analyzer.getType()));
        }

        if (!GenericValidator.isBlankOrNull(analyzer.getDescription())) {

            device.setNote(List.of(new Annotation().setText(analyzer.getDescription())));
        }

        if (!GenericValidator.isBlankOrNull(analyzer.getLocation())) {

            device.addExtension(new Extension("http://openelis.org/fhir/StructureDefinition/analyzer-location",
                    new StringType(analyzer.getLocation())));
        }

        if (analyzer.getCommunicationMode() != null) {

            CodeableConcept communicationModeConcept = new CodeableConcept();

            communicationModeConcept.addCoding(
                    new Coding().setSystem(COMM_MODE_CODESYSTEM).setCode(analyzer.getCommunicationMode().name())
                            .setDisplay(analyzer.getCommunicationMode().getLabel()));

            communicationModeConcept.setText(analyzer.getCommunicationMode().getLabel());

            device.addExtension(new Extension(COMM_MODE_EXTENSION, communicationModeConcept));
        }

        if (analyzer.getProtocolVersion() != null) {

            CodeableConcept protocolConcept = new CodeableConcept();

            protocolConcept.addCoding(new Coding()
                    .setSystem("http://openelis.org/fhir/CodeSystem/analyzer-protocol-version")
                    .setCode(analyzer.getProtocolVersion().name()).setDisplay(analyzer.getProtocolVersion().name()));

            protocolConcept.setText(analyzer.getProtocolVersion().name());

            device.addExtension(new Extension(PROTOCOL_EXTENSION, protocolConcept));
        }

        Extension transportExtension = new Extension(TRANSPORT_EXTENSION);

        if (!GenericValidator.isBlankOrNull(analyzer.getIpAddress())) {

            transportExtension.addExtension(new Extension("ipAddress", new StringType(analyzer.getIpAddress())));
        }

        if (analyzer.getPort() != null) {

            transportExtension.addExtension(new Extension("port", new IntegerType(analyzer.getPort())));
        }

        if (!GenericValidator.isBlankOrNull(analyzer.getImportDirectory())) {

            transportExtension
                    .addExtension(new Extension("importDirectory", new StringType(analyzer.getImportDirectory())));
        }

        if (!GenericValidator.isBlankOrNull(analyzer.getFilePattern())) {

            transportExtension.addExtension(new Extension("filePattern", new StringType(analyzer.getFilePattern())));
        }

        if (!GenericValidator.isBlankOrNull(analyzer.getFileFormat())) {

            transportExtension.addExtension(new Extension("fileFormat", new StringType(analyzer.getFileFormat())));
        }

        if (!GenericValidator.isBlankOrNull(analyzer.getDelimiter())) {

            transportExtension.addExtension(new Extension("delimiter", new StringType(analyzer.getDelimiter())));
        }

        if (analyzer.getHasHeader() != null) {

            transportExtension.addExtension(new Extension("hasHeader", new BooleanType(analyzer.getHasHeader())));
        }

        if (analyzer.getSkipRows() != null) {

            transportExtension.addExtension(new Extension("skipRows", new IntegerType(analyzer.getSkipRows())));
        }

        if (!transportExtension.getExtension().isEmpty()) {
            device.addExtension(transportExtension);
        }

        if (analyzer.getLastActivatedDate() != null) {

            device.addExtension(new Extension("http://openelis.org/fhir/StructureDefinition/analyzer-last-activated",
                    new DateTimeType(analyzer.getLastActivatedDate())));
        }

        if (!GenericValidator.isBlankOrNull(analyzer.getIdentifierPattern())) {

            device.addExtension(
                    new Extension("http://openelis.org/fhir/StructureDefinition/analyzer-identifier-pattern",
                            new StringType(analyzer.getIdentifierPattern())));
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
