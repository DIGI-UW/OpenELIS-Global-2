package org.openelisglobal.observationhistorytype;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Provides two way lookup of Observation History types from ID to Type and from
 * type to ID.
 *
 * @author pahill
 * @since 2010-04-12
 */
public class ObservationHistoryTypeMap {

    private static ObservationHistoryTypeMap s_instance = null;
    private final Map<String, String> type2Id;
    private final Map<String, String> id2Type;

    private ObservationHistoryTypeService observationHistoryTypeService = SpringContext
            .getBean(ObservationHistoryTypeService.class);

    public static ObservationHistoryTypeMap getInstance() {

        if (s_instance == null) {
            s_instance = new ObservationHistoryTypeMap();
        }
        return s_instance;
    }

    /**
     * Will force a new fetch of the map and any new PatientIdentityTypes in the DB
     * will be picked up Expected user will be the code which inserts new types into
     * the DB
     */
    public static void reset() {
        s_instance = null;
    }

    private ObservationHistoryTypeMap() {
        type2Id = new HashMap<>();
        id2Type = new HashMap<>();

        List<ObservationHistoryType> list = observationHistoryTypeService.getAll();

        for (ObservationHistoryType item : list) {
            mapNewItem(item);
        }
    }

    private void mapNewItem(ObservationHistoryType item) {
        type2Id.put(item.getTypeName(), item.getId());
        id2Type.put(item.getId(), item.getTypeName());
    }

    public String getIDForType(String type) {
        if (GenericValidator.isBlankOrNull(type)) {
            return null;
        }

        return type2Id.get(type);
    }

    public String getTypeFromId(String id) {
        if (GenericValidator.isBlankOrNull(id)) {
            return null;
        }
        return id2Type.get(id);
    }
}
