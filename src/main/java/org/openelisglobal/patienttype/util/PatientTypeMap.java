package org.openelisglobal.patienttype.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.patient.service.PatientTypeService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patienttype.valueholder.PatientType;
import org.openelisglobal.spring.util.SpringContext;

public class PatientTypeMap {

    private static PatientTypeMap s_instance = null;
    private Map<String, String> m_map;

    PatientTypeService patientTypeService = SpringContext.getBean(PatientTypeService.class);

    public static PatientTypeMap getInstance() {

        if (s_instance == null) {
            s_instance = new PatientTypeMap();
        }

        return s_instance;
    }

    /*
     * Will force the a new fetch of the map and any new PatientIdentityTypes in the
     * DB will be picked up
     *
     * Expected user will be the code which inserts new types into the DB
     */
    public static void reset() {
        s_instance = null;
    }

    @SuppressWarnings("unchecked")
    private PatientTypeMap() {
        m_map = new HashMap<>();
        List<PatientType> patientTypes = patientTypeService.getAllPatientTypes();

        for (PatientType patientType : patientTypes) {
            m_map.put(patientType.getType(), patientType.getId());
        }
    }

    public String getIDForType(String type) {
        return m_map.get(type);
    }

    public String getIdentityValue(List<PatientIdentity> identityList, String type) {

        String id = getIDForType(type);

        if (!GenericValidator.isBlankOrNull(id)) {
            for (PatientIdentity identity : identityList) {
                if (id.equals(identity.getIdentityTypeId())) {
                    return identity.getIdentityData();
                }
            }
        }
        return "";
    }
}
