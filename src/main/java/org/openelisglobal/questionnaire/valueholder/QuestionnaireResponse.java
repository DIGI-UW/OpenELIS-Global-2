package org.openelisglobal.questionnaire.valueholder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.Set;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.patient.valueholder.Patient;

@Entity
@Table(name = "questionnaire_response")
public class QuestionnaireResponse extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "questionnaire_response_generator")
    @SequenceGenerator(name = "questionnaire_response_generator", sequenceName = "questionnaire_response_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "fhir_uuid", unique = true)
    private UUID fhirUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaire_id")
    private Questionnaire questionnaire;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private QuestionnaireResponseStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Patient subject;

    @Column(name = "authored")
    private Timestamp authored;

    @Column(name = "summary")
    private String summary;

    @OneToMany(mappedBy = "questionnaireResponse", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuestionnaireResponseItem> items;

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public Questionnaire getQuestionnaire() {
        return questionnaire;
    }

    public void setQuestionnaire(Questionnaire questionnaire) {
        this.questionnaire = questionnaire;
    }

    public QuestionnaireResponseStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionnaireResponseStatus status) {
        this.status = status;
    }

    public Patient getSubject() {
        return subject;
    }

    public void setSubject(Patient subject) {
        this.subject = subject;
    }

    public Timestamp getAuthored() {
        return authored;
    }

    public void setAuthored(Timestamp authored) {
        this.authored = authored;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Set<QuestionnaireResponseItem> getItems() {
        return items;
    }

    public void setItems(Set<QuestionnaireResponseItem> items) {
        this.items = items;
    }

    public enum QuestionnaireResponseStatus {

        IN_PROGRESS("in-progress"), COMPLETED("completed"), AMENDED("amended"), STOPPED("stopped"),
        ENTERED_IN_ERROR("entered-in-error");

        private final String value;

        QuestionnaireResponseStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @Override
    public Integer getId() {
        return this.id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

}
