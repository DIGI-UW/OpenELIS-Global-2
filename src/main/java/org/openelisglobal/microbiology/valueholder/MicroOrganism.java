package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_organism", schema = "clinlims")
public class MicroOrganism extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "short_name", length = 100)
    private String shortName;

    @Column(name = "whonet_code", length = 50)
    private String whonetCode;

    @Column(name = "ocl_code", length = 100)
    private String oclCode;

    @Column(name = "organism_group", length = 100)
    private String organismGroup;

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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getWhonetCode() {
        return whonetCode;
    }

    public void setWhonetCode(String whonetCode) {
        this.whonetCode = whonetCode;
    }

    public String getOclCode() {
        return oclCode;
    }

    public void setOclCode(String oclCode) {
        this.oclCode = oclCode;
    }

    public String getOrganismGroup() {
        return organismGroup;
    }

    public void setOrganismGroup(String organismGroup) {
        this.organismGroup = organismGroup;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
}
