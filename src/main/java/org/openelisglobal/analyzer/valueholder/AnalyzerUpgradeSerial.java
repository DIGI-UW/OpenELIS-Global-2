package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

/**
 * Identifies retained serial configuration requiring explicit review in this
 * scoped upgrade.
 */
@Entity
@Immutable
@Table(name = "serial_port_configuration")
public class AnalyzerUpgradeSerial {
    @Id
    @Column(name = "id")
    private String id;
    @Column(name = "analyzer_id")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String analyzerId;
    @Column(name = "active")
    private boolean active;
}
