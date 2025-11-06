package org.openelisglobal.observationhistorytype.valueholder;

import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.SimpleBaseEntity;

/**
 * DemographicHistoryType entity.
 *
 * @author MyEclipse Persistence Tools
 * @author pahill
 * @since 2010-04-09
 */
public class ObservationHistoryType extends BaseObject<String> implements SimpleBaseEntity<String> {
    private static final long serialVersionUID = 1L;

    // Fields

    private String id;
    private String typeName;
    private String description;

    // Constructors

    public ObservationHistoryType() {
    }

    /** minimal constructor */
    public ObservationHistoryType(String id, String typeName, String description) {
        this.id = id;
        this.typeName = typeName;
        this.description = description;
    }

    /** full constructor */
    public ObservationHistoryType(String id, String typeName, String description, Timestamp lastupdated) {
        this.id = id;
        this.typeName = typeName;
        this.description = description;
        setLastupdated(lastupdated);
    }

    // Property accessors

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
