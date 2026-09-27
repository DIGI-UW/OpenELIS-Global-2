package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "analyzer_profile_binding", uniqueConstraints = @UniqueConstraint(name = "uq_analyzer_profile_binding_revision", columnNames = {
        "profile_id", "profile_revision" }))
@DynamicUpdate
public class AnalyzerProfileBinding extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "analyzer_profile_binding_seq_gen")
    @GenericGenerator(name = "analyzer_profile_binding_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "analyzer_profile_binding_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @Column(name = "profile_id", length = 128, nullable = false)
    private String profileId;

    @Min(1)
    @Column(name = "profile_revision", nullable = false)
    private int profileRevision;

    @Pattern(regexp = "^sha256:[0-9a-f]{64}$")
    @Column(name = "profile_fingerprint", length = 71, nullable = false)
    private String profileFingerprint;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public int getProfileRevision() {
        return profileRevision;
    }

    public void setProfileRevision(int profileRevision) {
        this.profileRevision = profileRevision;
    }

    public String getProfileFingerprint() {
        return profileFingerprint;
    }

    public void setProfileFingerprint(String profileFingerprint) {
        this.profileFingerprint = profileFingerprint;
    }
}
