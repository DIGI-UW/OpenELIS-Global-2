package org.openelisglobal.common.util;

/**
 * Keys used by the Location Reporting feature.
 *
 * Two kinds of keys: - PROPERTY_*: keys expected in application.properties (the
 * GOFR commented block) - SITEINFO_*: keys used for entries in the
 * site_information table (backwards-compatible)
 */
public class LocationReportingConfigurationProperties {

    public static final String PROPERTY_FHIR_STORE = "org.openelisglobal.locationreporting.fhirstore";
    public static final String PROPERTY_AUTH_URL = "org.openelisglobal.locationreporting.authurl";
    public static final String PROPERTY_USERNAME = "org.openelisglobal.locationreporting.username";
    public static final String PROPERTY_PASSWORD = "org.openelisglobal.locationreporting.password";
    public static final String PROPERTY_AUTH = "org.openelisglobal.locationreporting.auth";
    public static final String PROPERTY_SCHEDULE = "org.openelisglobal.locationreporting.schedule";

    public static final String PROPERTY_OPT_IN = "locationReporting.optIn";
    public static final String PROPERTY_LAST_SENT = "locationReporting.lastSent";

    public static final String SITEINFO_OPT_IN = "locationReportingOptIn";
    public static final String SITEINFO_LAST_SENT = "lastLocationReportDate";
}