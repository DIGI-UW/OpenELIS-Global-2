package org.openelisglobal.fhir.serviceImpl;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Device.DeviceDeviceNameComponent;
import org.hl7.fhir.r4.model.Device.DeviceNameType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.dataexchange.fhir.FHIRTransformUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.fhir.service.DeviceTransformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeviceTransformServiceImpl implements DeviceTransformService {
    @Autowired
    private FHIRTransformUtil fhirTransformUtil;
    @Autowired
    private FhirConfig fhirConfig;

    @Override
    public Device transformAnalyzerToDevice(Analyzer analyzer) {
        Device device = new Device();
        // ensureFhirUuid() generates a UUID if missing (shouldn't happen with backfill
        // migration)
        String fhirUuid = analyzer.ensureFhirUuid();
        device.setId(fhirUuid);

        device.addIdentifier(
                fhirTransformUtil.createIdentifier(fhirConfig.getOeFhirSystem() + "/analyzer_uuid", fhirUuid));

        if (!GenericValidator.isBlankOrNull(analyzer.getMachineId())) {
            device.addIdentifier(fhirTransformUtil
                    .createIdentifier(fhirConfig.getOeFhirSystem() + "/analyzer_machineId", analyzer.getMachineId()));
            device.setSerialNumber(analyzer.getMachineId());
        }

        if (!GenericValidator.isBlankOrNull(analyzer.getDiscoveredSourceId())) {
            device.addIdentifier(fhirTransformUtil.createIdentifier(fhirConfig.getOeFhirSystem() + "/analyzer_sourceId",
                    analyzer.getDiscoveredSourceId()));
        }

        if (!GenericValidator.isBlankOrNull(analyzer.getName())) {
            device.addDeviceName(new DeviceDeviceNameComponent().setName(analyzer.getName())
                    .setType(DeviceNameType.USERFRIENDLYNAME));
        }

        if (!GenericValidator.isBlankOrNull(analyzer.getType())) {
            device.setType(new CodeableConcept().setText(analyzer.getType()));
        }

        Identifier facilityId = fhirTransformUtil.createFacilityIdentifier();
        if (facilityId != null) {
            device.setOwner(new Reference().setIdentifier(facilityId));
        }

        return device;
    }

}
