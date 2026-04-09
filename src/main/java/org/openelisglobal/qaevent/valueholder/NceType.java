package org.openelisglobal.qaevent.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.localization.valueholder.Localization;

@Entity
@Table(name = "nce_type", schema = "clinlims")
public class NceType extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nce_type_generator")
    @SequenceGenerator(name = "nce_type_generator", sequenceName = "nce_type_id_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "display_key", length = 100)
    private String displayKey;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "active")
    private Boolean active;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "name_localization_id", referencedColumnName = "id")
    private Localization nameLocalization;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
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

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NceType nceType = (NceType) o;
        return Objects.equals(id, nceType.id) && Objects.equals(name, nceType.name)
                && Objects.equals(displayKey, nceType.displayKey) && Objects.equals(active, nceType.active);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, displayKey, active);
    }
}
