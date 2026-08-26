package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_culture_setup", schema = "clinlims")
public class MicroCultureSetup extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "method_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String methodId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "workflow_type", nullable = false, length = 40)
    private String workflowType;

    @Column(name = "media_defaults")
    private String mediaDefaults;

    @Column(name = "incubation_defaults")
    private String incubationDefaults;

    @Column(name = "atmosphere_defaults")
    private String atmosphereDefaults;

    @Column(name = "is_active", nullable = false, length = 2)
    private String isActive = "Y";

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getMethodId() {
        return methodId;
    }

    public void setMethodId(String methodId) {
        this.methodId = methodId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorkflowType() {
        return workflowType;
    }

    public void setWorkflowType(String workflowType) {
        this.workflowType = workflowType;
    }

    public String getMediaDefaults() {
        return mediaDefaults;
    }

    public void setMediaDefaults(String mediaDefaults) {
        this.mediaDefaults = mediaDefaults;
    }

    public String getIncubationDefaults() {
        return incubationDefaults;
    }

    public void setIncubationDefaults(String incubationDefaults) {
        this.incubationDefaults = incubationDefaults;
    }

    public String getAtmosphereDefaults() {
        return atmosphereDefaults;
    }

    public void setAtmosphereDefaults(String atmosphereDefaults) {
        this.atmosphereDefaults = atmosphereDefaults;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
}
