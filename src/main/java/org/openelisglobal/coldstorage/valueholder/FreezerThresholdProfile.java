package org.openelisglobal.coldstorage.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

@Getter
@Setter
@Entity
@Table(name = "freezer_threshold_profile")
public class FreezerThresholdProfile extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "freezer_threshold_profile_generator")
    @SequenceGenerator(name = "freezer_threshold_profile_generator", sequenceName = "freezer_threshold_profile_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freezer_id", nullable = false)
    private Freezer freezer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "threshold_profile_id", nullable = false)
    private ThresholdProfile thresholdProfile;

    @Column(name = "effective_start", nullable = false)
    private OffsetDateTime effectiveStart;

    @Column(name = "effective_end")
    private OffsetDateTime effectiveEnd;

    @Column(name = "is_default")
    private Boolean isDefault = Boolean.FALSE;
}
