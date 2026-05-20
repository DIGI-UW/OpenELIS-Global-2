package org.openelisglobal.questionnaire.valueholder;

import jakarta.persistence.CascadeType;
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
import java.util.Set;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "questionnaire_response_item")
public class QuestionnaireResponseItem extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "questionnaire_response_item_generator")
    @SequenceGenerator(name = "questionnaire_response_item_generator", sequenceName = "questionnaire_response_item_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaire_response_id")
    private QuestionnaireResponse questionnaireResponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_item_id")
    private QuestionnaireResponseItem parentItem;

    @OneToMany(mappedBy = "parentItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuestionnaireResponseItem> items;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuestionnaireResponseAnswer> answers;

    private String linkId;

    private String text;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public QuestionnaireResponse getQuestionnaireResponse() {
        return questionnaireResponse;
    }

    public void setQuestionnaireResponse(QuestionnaireResponse questionnaireResponse) {
        this.questionnaireResponse = questionnaireResponse;
    }

    public QuestionnaireResponseItem getParentItem() {
        return parentItem;
    }

    public void setParentItem(QuestionnaireResponseItem parentItem) {
        this.parentItem = parentItem;
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Set<QuestionnaireResponseAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(Set<QuestionnaireResponseAnswer> answers) {
        this.answers = answers;
    }

    public Set<QuestionnaireResponseItem> getItems() {
        return items;
    }

    public void setItems(Set<QuestionnaireResponseItem> items) {
        this.items = items;
    }

}