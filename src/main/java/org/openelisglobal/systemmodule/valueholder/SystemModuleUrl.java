package org.openelisglobal.systemmodule.valueholder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

// This class defines the urls that correspond to the different modules
@Entity
@Table(name = "system_module_url", schema = "clinlims")
public class SystemModuleUrl extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "system_module_url_seq_gen")
    @GenericGenerator(name = "system_module_url_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "system_module_url_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "id", precision = 10, scale = 0)
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
