package org.openelisglobal.questionnaire.valueholder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.sql.Date;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "questionnaire")
public class Questionnaire extends BaseObject<String> {

    @Id
    @Column(name = "ID", precision = 10, scale = 0)
    @GeneratedValue(generator = "questionnaire_seq_gen")
    @GenericGenerator(name = "questionnaire_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "questionnaire_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "has_item")
    private boolean hasItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_questionnaire_id")
    private Questionnaire parentQuestionnaire;

    @OneToMany(mappedBy = "questionnaire", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuestionnaireItem> questionnaireItems;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private QuestionaireStatus status;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "questionnaire_name", length = 36, nullable = false)
    private String questionnaireName;

    @Column(name = "approval_date")
    private Date approvalDate;

    @Column(name = "effective_date")
    private Date effectiveDate;

    @Column(name = "last_reviewed")
    private Date lastReviewed;

    @Column(name = "status_id", length = 10)
    private String statusId;

    @Column(name = "purpose", nullable = false, length = 255)
    private String purpose;

    public Questionnaire() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isHasItem() {
        return hasItem;
    }

    public void setHasItem(boolean hasItem) {
        this.hasItem = hasItem;
    }

    public Questionnaire getParentQuestionnaire() {
        return parentQuestionnaire;
    }

    public void setParentQuestionnaire(Questionnaire parentQuestionnaire) {
        this.parentQuestionnaire = parentQuestionnaire;
    }

    public Set<QuestionnaireItem> getQuestionnaireItems() {
        return questionnaireItems;
    }

    public void setQuestionnaireItems(Set<QuestionnaireItem> questionnaireItems) {
        this.questionnaireItems = questionnaireItems;
    }

    public QuestionaireStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionaireStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getQuestionnaireName() {
        return questionnaireName;
    }

    public void setQuestionnaireName(String questionnaireName) {
        this.questionnaireName = questionnaireName;
    }

    public Date getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(Date approvalDate) {
        this.approvalDate = approvalDate;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Date getLastReviewed() {
        return lastReviewed;
    }

    public void setLastReviewed(Date lastReviewed) {
        this.lastReviewed = lastReviewed;
    }

    public String getStatusId() {
        return statusId;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public enum QuestionaireStatus {
        DRAFT("Draft"), ACTIVE("Active"), RETIRED("Retired"), UNKNOWN("Unknown");

        private final String display;

        QuestionaireStatus(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
}