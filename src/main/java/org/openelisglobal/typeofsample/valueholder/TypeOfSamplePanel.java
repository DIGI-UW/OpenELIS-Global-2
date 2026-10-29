package org.openelisglobal.typeofsample.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Setter
@Getter
@DynamicUpdate
@Entity
@Table(name = "SAMPLETYPE_PANEL")
public class TypeOfSamplePanel extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "SAMPLE_TYPE_PANEL_SEQ_GEN")
    @GenericGenerator(name = "SAMPLE_TYPE_PANEL_SEQ_GEN", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "SAMPLE_TYPE_PANEL_SEQ"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;

    @Column(name = "SAMPLE_TYPE_ID", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String typeOfSampleId;

    @Column(name = "PANEL_ID", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String panelId;

    @Transient
    @Override
    public Timestamp getLastupdated() {
        return null;
    }

    @Override
    public void setLastupdated(Timestamp lastupdated) {
    }
}
