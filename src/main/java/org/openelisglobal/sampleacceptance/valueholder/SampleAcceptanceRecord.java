package org.openelisglobal.sampleacceptance.valueholder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.valueholder.BaseObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Immutable per-specimen Sample Acceptance Record (S-09 / OGC-580), keyed to a
 * {@code sample_item} (one physical specimen) so each specimen is accepted /
 * reviewed individually at the Collect (reception) step. Each assessment
 * inserts a NEW row — corrections never overwrite, so the row set is the audit
 * trail and the latest row (highest id) is the current decision. Distinct from
 * {@code SampleQaChecklist} (OGC-356), which is one mutable boolean record per
 * sample.
 */
@Entity
@Table(name = "sample_acceptance_record")
public class SampleAcceptanceRecord extends BaseObject<Integer> {

    public static final String ANSWER_PASS = "PASS";
    public static final String ANSWER_FAIL = "FAIL";
    public static final String ANSWER_NA = "NA";

    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REVIEW = "REVIEW";
    public static final String STATUS_PENDING = "PENDING";

    private static final Logger logger = LoggerFactory.getLogger(SampleAcceptanceRecord.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sample_acceptance_record_seq")
    @SequenceGenerator(name = "sample_acceptance_record_seq", sequenceName = "sample_acceptance_record_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "sample_item_id", nullable = false)
    private Integer sampleItemId;

    /**
     * Resolved domain snapshot: CLINICAL / ENVIRONMENTAL / VECTOR, or null =
     * lab-wide.
     */
    @Column(name = "domain")
    private String domain;

    @Column(name = "answers")
    private String answersJson;

    @Column(name = "overall_status", nullable = false)
    private String overallStatus;

    @Column(name = "assessed_by_user_id")
    private Integer assessedByUserId;

    @Column(name = "assessed_at")
    private Timestamp assessedAt;

    @Transient
    private List<Answer> answers;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(Integer sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getAnswersJson() {
        return answersJson;
    }

    public void setAnswersJson(String answersJson) {
        this.answersJson = answersJson;
        this.answers = null; // clear cache
    }

    public List<Answer> getAnswers() {
        if (answers == null && answersJson != null) {
            try {
                answers = objectMapper.readValue(answersJson, new TypeReference<List<Answer>>() {
                });
            } catch (JsonProcessingException e) {
                logger.error("Error parsing acceptance answers JSON", e);
                answers = new ArrayList<>();
            }
        }
        return answers != null ? answers : new ArrayList<>();
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
        try {
            this.answersJson = objectMapper.writeValueAsString(answers != null ? answers : new ArrayList<>());
        } catch (JsonProcessingException e) {
            logger.error("Error serializing acceptance answers to JSON", e);
            this.answersJson = "[]";
        }
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public Integer getAssessedByUserId() {
        return assessedByUserId;
    }

    public void setAssessedByUserId(Integer assessedByUserId) {
        this.assessedByUserId = assessedByUserId;
    }

    public Timestamp getAssessedAt() {
        return assessedAt;
    }

    public void setAssessedAt(Timestamp assessedAt) {
        this.assessedAt = assessedAt;
    }

    @PrePersist
    protected void onInsert() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (assessedAt == null) {
            assessedAt = now;
        }
        setLastupdated(now);
    }

    /** One answered checklist item; the label is snapshotted at assessment time. */
    public static class Answer {
        private String itemKey;
        private String label;
        private String answer; // PASS | FAIL | NA
        private String note;

        public Answer() {
        }

        public Answer(String itemKey, String label, String answer, String note) {
            this.itemKey = itemKey;
            this.label = label;
            this.answer = answer;
            this.note = note;
        }

        public String getItemKey() {
            return itemKey;
        }

        public void setItemKey(String itemKey) {
            this.itemKey = itemKey;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }
}
