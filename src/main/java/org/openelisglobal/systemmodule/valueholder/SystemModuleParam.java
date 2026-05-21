package org.openelisglobal.systemmodule.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "system_module_param", schema = "clinlims")
public class SystemModuleParam extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "system_module_param_seq_gen")
    @GenericGenerator(name = "system_module_param_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "system_module_param_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "id", precision = 10, scale = 0)
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
