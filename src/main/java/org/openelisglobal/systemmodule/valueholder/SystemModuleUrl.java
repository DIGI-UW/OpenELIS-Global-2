package org.openelisglobal.systemmodule.valueholder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.converter.StringToIntegerConverter;

// This class defines the urls that correspond to the different modules
@Entity
@Table(name = "system_module_url", schema = "clinlims")
public class SystemModuleUrl extends BaseObject<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "system_module_url_seq_gen")
    @SequenceGenerator(name = "system_module_url_seq_gen", sequenceName = "system_module_url_seq", schema = "clinlims", allocationSize = 1)
    @Convert(converter = StringToIntegerConverter.class)
    @Column(name = "id")
    private String id;

    @Column(name = "url_path", nullable = false)
    private String urlPath;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "system_module_id")
    private SystemModule systemModule;

    // the needed request parameters that need to be present to access the module
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "system_module_param_id")
    private SystemModuleParam param;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public void setUrlPath(String urlPath) {
        this.urlPath = urlPath;
    }

    public SystemModule getSystemModule() {
        return systemModule;
    }

    public void setSystemModule(SystemModule systemModule) {
        this.systemModule = systemModule;
    }

    public SystemModuleParam getParam() {
        return param;
    }

    public void setParam(SystemModuleParam param) {
        this.param = param;
    }
}
