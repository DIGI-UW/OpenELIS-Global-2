package org.openelisglobal.fhir.search.searchparams;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import org.hl7.fhir.r4.model.Device;

/**
 * Search parameters the Device facade answers from the OpenELIS analyzer table:
 * logical id, identifiers (uuid, machine id, discovered source id), device
 * name, type, status and last-updated timestamp.
 */
public class DeviceSearchParams extends BaseSearchParam {

    private StringAndListParam deviceName;

    private TokenAndListParam type;

    private TokenAndListParam status;

    public DeviceSearchParams(TokenAndListParam id, TokenAndListParam identifier, StringAndListParam deviceName,
            TokenAndListParam type, TokenAndListParam status, DateRangeParam lastUpdated, SortSpec sort) {

        super(id, identifier, lastUpdated, sort);

        this.deviceName = deviceName;
        this.type = type;
        this.status = status;
    }

    @Override
    public SearchParameterMap toSearchParameterMap() {

        SearchParameterMap map = new SearchParameterMap();
        addBaseSearchParameters(map);

        if (deviceName != null) {
            map.addParameter(Device.SP_DEVICE_NAME, deviceName);
        }
        if (type != null) {
            map.addParameter(Device.SP_TYPE, type);
        }
        if (status != null) {
            map.addParameter(Device.SP_STATUS, status);
        }
        if (getSort() != null) {
            map.setSortSpec(getSort());
        }
        return map;
    }

    public StringAndListParam getDeviceName() {
        return deviceName;
    }

    public TokenAndListParam getType() {
        return type;
    }

    public TokenAndListParam getStatus() {
        return status;
    }
}
