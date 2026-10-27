package org.openelisglobal.addon.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.security.converter.EncryptionConverter;

/** A server-side key of the add-on mechanism (encrypted at rest). */
@Entity
@Table(name = "addon_secret", schema = "clinlims")
public class AddonSecret extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "name", length = 64)
    private String name;

    @Column(name = "value", nullable = false, length = 512)
    @Convert(converter = EncryptionConverter.class)
    private String value;

    @Override
    public String getId() {
        return name;
    }

    @Override
    public void setId(String id) {
        this.name = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
