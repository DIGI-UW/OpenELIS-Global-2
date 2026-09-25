package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Read-only upgrade input. Remove only after verified transfer and source
 * cleanup.
 */
@Entity
@Immutable
@Table(name = "analyzer")
public class AnalyzerUpgradeSource extends BaseObject<String> {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id", insertable = false, updatable = false)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;
    @Column(name = "analyzer_type_id", insertable = false, updatable = false)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String typeId;
    @Column(name = "profile_binding_id", insertable = false, updatable = false)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String profileBindingId;
    @Column(name = "ip_address", insertable = false, updatable = false)
    private String host;
    @Column(name = "port", insertable = false, updatable = false)
    private Integer port;
    @Column(name = "communication_mode", insertable = false, updatable = false)
    private String communicationMode;
    @Column(name = "import_directory", insertable = false, updatable = false)
    private String directory;
    @Column(name = "file_pattern", insertable = false, updatable = false)
    private String filePattern;
    @Column(name = "file_format", insertable = false, updatable = false)
    private String fileFormat;
    @Column(name = "column_mappings_json", insertable = false, updatable = false)
    private String columnMappings;

    public String getColumnMappings() {
        return columnMappings;
    }

    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getTypeId() {
        return typeId;
    }

    public String getProfileBindingId() {
        return profileBindingId;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getCommunicationMode() {
        return communicationMode;
    }

    public String getDirectory() {
        return directory;
    }

    public String getFilePattern() {
        return filePattern;
    }

    public String getFileFormat() {
        return fileFormat;
    }
}
