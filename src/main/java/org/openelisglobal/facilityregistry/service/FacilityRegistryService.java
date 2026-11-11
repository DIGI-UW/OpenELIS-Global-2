package org.openelisglobal.facilityregistry.service;

import java.math.BigDecimal;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Organization;
import org.openelisglobal.facilityregistry.form.FacilityLocationStatus;

/**
 * Registers this OpenELIS instance as a facility in a facility registry such as
 * GOFR: an IHE mCSD facility Organization and a facility Location carrying the
 * instance's coordinates, so a registry map shows every laboratory.
 *
 * <p>
 * Coordinates follow one of two modes chosen by
 * {@code org.openelisglobal.facility.autolocation}. In manual mode (false or
 * empty) they come only from {@code org.openelisglobal.facility.latitude} and
 * {@code longitude}. In automatic mode (true) they come only from detection: an
 * IP lookup at startup, replaced by the position an administrator's browser
 * reports.
 */
public interface FacilityRegistryService {

    String SOURCE_PROPERTIES = "properties";
    String SOURCE_IP = "ip";
    String SOURCE_BROWSER = "browser";

    String MODE_AUTO = "AUTO";
    String MODE_MANUAL = "MANUAL";

    String FACILITY_LOCATION_PROFILE = "http://ihe.net/fhir/StructureDefinition/IHE.mCSD.FacilityLocation";
    String FACILITY_ORGANIZATION_PROFILE = "http://ihe.net/fhir/StructureDefinition/IHE.mCSD.FacilityOrganization";
    String ROLE_CODE_SYSTEM = "http://terminology.hl7.org/CodeSystem/v3-RoleCode";
    String MCSD_TYPE_SYSTEM = "urn:ietf:rfc:3986";
    String MCSD_FACILITY_CODE = "urn:ihe:iti:mcsd:2019:facility";
    String PHYSICAL_TYPE_SYSTEM = "http://hl7.org/fhir/ValueSet/location-physical-type";
    String PHYSICAL_TYPE_BUILDING = "bu";

    /**
     * The coordinates in effect, their source, the mode and the last registration.
     */
    FacilityLocationStatus getStatus();

    /**
     * Stores the position an administrator's browser reported and registers it.
     *
     * @throws FacilityLocationModeException when the location is in manual mode
     * @throws IllegalArgumentException      when a coordinate is missing or out of
     *                                       range
     */
    FacilityLocationStatus recordBrowserLocation(BigDecimal latitude, BigDecimal longitude, String sysUserId);

    /**
     * Brings the stored coordinates in line with the mode and registers the
     * facility when it is not registered yet or something it registered changed.
     * Runs shortly after startup and then on a schedule, so a registry that was
     * down is retried.
     */
    void synchronize();

    /** Sends the facility Organization and Location to the registry now. */
    boolean register();

    /** The mCSD facility Organization as the registry receives it. */
    Organization buildRegistryOrganization();

    /** The mCSD facility Location, with a position when coordinates are known. */
    Location buildRegistryLocation(BigDecimal latitude, BigDecimal longitude);
}
