package org.openelisglobal.qaevent.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.localization.valueholder.Localization;

@Entity
@Table(name = "nce_category", schema = "clinlims")
public class NceCategory extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nce_category_generator")
    @SequenceGenerator(name = "nce_category_generator", sequenceName = "nce_category_id_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "display_key", length = 100)
    private String displayKey;

    @Column(name = "active")
    private Boolean active;

    @Version
    @Column(name = "last_updated")
    private Timestamp lastupdated;

    // TODO: Add name_localization_id column to database before enabling
    @Transient
    private Localization nameLocalization;

    @Override
    public String getId() {
        return id != null ? String.valueOf(id) : null;
    }

    @Override
    public void setId(String id) {
        this.id = id != null ? Integer.valueOf(id) : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayKey() {
        return displayKey;
    }

    public void setDisplayKey(String displayKey) {
        this.displayKey = displayKey;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Timestamp getLastupdated() {
        return lastupdated;
    }

    public void setLastupdated(Timestamp lastupdated) {
        this.lastupdated = lastupdated;
    }

    public Localization getNameLocalization() {
        return nameLocalization;
    }

    public void setNameLocalization(Localization nameLocalization) {
        this.nameLocalization = nameLocalization;
    }

    /**
     * Get the localized name for the current locale. Falls back to the name field
     * if no localization is set.
     */
    public String getLocalizedName() {
        if (nameLocalization != null) {
            String localized = nameLocalization.getLocalizedValue();
            if (localized != null && !localized.isEmpty()) {
                return localized;
            }
        }
        return name;
    }
}
