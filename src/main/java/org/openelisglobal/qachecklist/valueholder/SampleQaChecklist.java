package org.openelisglobal.qachecklist.valueholder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.common.valueholder.BaseObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SampleQaChecklist entity - Tracks QA verification status for orders in Step 4
 * of the workflow. Verified items are stored as JSON for flexibility with
 * configurable checklist items.
 */
@Entity
@Table(name = "SAMPLE_QA_CHECKLIST")
public class SampleQaChecklist extends BaseObject<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(SampleQaChecklist.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sample_qa_checklist_seq")
    @SequenceGenerator(name = "sample_qa_checklist_seq", sequenceName = "sample_qa_checklist_seq", allocationSize = 1)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "SAMPLE_ID", nullable = false, unique = true)
    private Integer sampleId;

    @Column(name = "VERIFIED_ITEMS")
    private String verifiedItemsJson;

    @Column(name = "ALL_REQUIRED_VERIFIED", nullable = false)
    private Boolean allRequiredVerified = false;

    @Column(name = "VERIFIED_BY_USER_ID")
    private Integer verifiedByUserId;

    @Column(name = "VERIFIED_DATE")
    private Timestamp verifiedDate;

    @Transient
    private Map<String, Boolean> verifiedItems;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSampleId() {
        return sampleId;
    }

    public void setSampleId(Integer sampleId) {
        this.sampleId = sampleId;
    }

    public String getVerifiedItemsJson() {
        return verifiedItemsJson;
    }

    public void setVerifiedItemsJson(String verifiedItemsJson) {
        this.verifiedItemsJson = verifiedItemsJson;
        this.verifiedItems = null; // Clear cached map
    }

    /**
     * Get verified items as a map.
     */
    public Map<String, Boolean> getVerifiedItems() {
        if (verifiedItems == null && verifiedItemsJson != null) {
            try {
                verifiedItems = objectMapper.readValue(verifiedItemsJson, new TypeReference<Map<String, Boolean>>() {
                });
            } catch (JsonProcessingException e) {
                logger.error("Error parsing verified items JSON", e);
                verifiedItems = new HashMap<>();
            }
        }
        return verifiedItems != null ? verifiedItems : new HashMap<>();
    }

    /**
     * Set verified items from a map.
     */
    public void setVerifiedItems(Map<String, Boolean> verifiedItems) {
        this.verifiedItems = verifiedItems;
        try {
            this.verifiedItemsJson = objectMapper.writeValueAsString(verifiedItems);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing verified items to JSON", e);
            this.verifiedItemsJson = "{}";
        }
    }

    /**
     * Check if a specific item is verified.
     */
    public boolean isItemVerified(String itemKey) {
        Map<String, Boolean> items = getVerifiedItems();
        return Boolean.TRUE.equals(items.get(itemKey));
    }

    /**
     * Set verification status for a specific item.
     */
    public void setItemVerified(String itemKey, boolean verified) {
        Map<String, Boolean> items = getVerifiedItems();
        if (items == null) {
            items = new HashMap<>();
        }
        items.put(itemKey, verified);
        setVerifiedItems(items);
    }

    public Boolean getAllRequiredVerified() {
        return allRequiredVerified;
    }

    public void setAllRequiredVerified(Boolean allRequiredVerified) {
        this.allRequiredVerified = allRequiredVerified;
    }

    public Integer getVerifiedByUserId() {
        return verifiedByUserId;
    }

    public void setVerifiedByUserId(Integer verifiedByUserId) {
        this.verifiedByUserId = verifiedByUserId;
    }

    public Timestamp getVerifiedDate() {
        return verifiedDate;
    }

    public void setVerifiedDate(Timestamp verifiedDate) {
        this.verifiedDate = verifiedDate;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        setLastupdated(now);
        if (Boolean.TRUE.equals(allRequiredVerified) && verifiedDate == null) {
            verifiedDate = now;
        }
    }
}
