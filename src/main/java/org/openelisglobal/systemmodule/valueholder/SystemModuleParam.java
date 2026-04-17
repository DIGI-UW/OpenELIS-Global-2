package org.openelisglobal.systemmodule.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.converter.StringToIntegerConverter;

@Entity
@Table(name = "system_module_param", schema = "clinlims")
public class SystemModuleParam extends BaseObject<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "system_module_param_seq_gen")
    @SequenceGenerator(name = "system_module_param_seq_gen", sequenceName = "system_module_param_seq", schema = "clinlims", allocationSize = 1)
    @Convert(converter = StringToIntegerConverter.class)
    @Column(name = "id")
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "value", nullable = false)
    private String value;

    // urls is the inverse side — not mapped (no FK on this side)
    private transient Set<SystemModuleUrl> urls = new HashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Set<SystemModuleUrl> getUrls() {
        return urls;
    }

    public void setUrls(Set<SystemModuleUrl> urls) {
        this.urls = urls;
    }
}
