package org.openelisglobal.questionnaire.valueholder;

import jakarta.persistence.*;
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
    @JoinColumn(name = "questionnaire_response_id", nullable = false)
    private QuestionnaireResponse questionnaireResponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_item_id")
    private QuestionnaireResponseItem parentItem;

    @OneToMany(mappedBy = "parentItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuestionnaireResponseItem> childItems;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuestionnaireResponseAnswer> answers;

    @Column(name = "link_id")
    private String linkId;

    @Column(name = "text")
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

    public Set<QuestionnaireResponseItem> getChildItems() {
        return childItems;
    }

    public void setChildItems(Set<QuestionnaireResponseItem> childItems) {
        this.childItems = childItems;
    }

    public Set<QuestionnaireResponseAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(Set<QuestionnaireResponseAnswer> answers) {
        this.answers = answers;
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
}