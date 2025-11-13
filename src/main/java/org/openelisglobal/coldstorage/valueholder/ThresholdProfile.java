package org.openelisglobal.coldstorage.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@Getter
@Setter
@Entity
@Table(name = "threshold_profile")
public class ThresholdProfile extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "threshold_profile_generator")
    @SequenceGenerator(name = "threshold_profile_generator", sequenceName = "threshold_profile_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 128)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "warning_min")
    private BigDecimal warningMin;

    @Column(name = "warning_max")
    private BigDecimal warningMax;

    @Column(name = "critical_min")
    private BigDecimal criticalMin;

    @Column(name = "critical_max")
    private BigDecimal criticalMax;

    @Column(name = "min_excursion_minutes")
    private Integer minExcursionMinutes = 5;

    @Column(name = "max_duration_minutes")
    private Integer maxDurationMinutes;

    @Column(name = "humidity_warning_min")
    private BigDecimal humidityWarningMin;

    @Column(name = "humidity_warning_max")
    private BigDecimal humidityWarningMax;

    @Column(name = "humidity_critical_min")
    private BigDecimal humidityCriticalMin;

    @Column(name = "humidity_critical_max")
    private BigDecimal humidityCriticalMax;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private SystemUser createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "thresholdProfile", fetch = FetchType.LAZY)
    private List<FreezerThresholdProfile> assignments = new ArrayList<>();

    /**
     * Convenience method to get the created by user ID.
     *
     * @return user ID or null if createdBy is not set
     */
    public String getCreatedByUserId() {
        return createdBy != null ? createdBy.getId() : null;
    }

    /**
     * Convenience method to set the created by user by ID. Note: This creates a
     * proxy reference. For full object, use setCreatedBy(SystemUser).
     *
     * @param userId the user ID to set
     */
    public void setCreatedByUserId(String userId) {
        if (userId != null) {
            SystemUser userRef = new SystemUser();
            userRef.setId(userId);
            this.createdBy = userRef;
        } else {
            this.createdBy = null;
        }
    }
}
