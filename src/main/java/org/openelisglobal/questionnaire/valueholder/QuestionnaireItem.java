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
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "questionnaire_item")
public class QuestionnaireItem extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "questionnaire_item_generator")
    @SequenceGenerator(name = "questionnaire_item_generator", sequenceName = "questionnaire_item_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaire_id")
    private Questionnaire questionnaire;

    @Column(name = "link_id", nullable = false, length = 255)
    private String linkId;

    @Column(name = "text", length = 1000)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    @NotNull
    private QuestionnaireItemType itemType;

    @Column(name = "required")
    private boolean required;

    @Column(name = "repeats")
    private boolean repeats;

    @Column(name = "item_order")
    private Integer itemOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_item_id")
    private QuestionnaireItem parentItem;

    @OneToMany(mappedBy = "parentItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuestionnaireItem> childItems;

    @OneToMany(mappedBy = "questionnaireItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuestionnaireAnswerOption> answerOptions;

    @OneToMany(mappedBy = "questionnaireItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuestionnaireItemInitial> initials;

    public QuestionnaireItem() {
    }

    @Override
    public Integer getId() {
        return this.id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Questionnaire getQuestionnaire() {
        return questionnaire;
    }

    public void setQuestionnaire(Questionnaire questionnaire) {
        this.questionnaire = questionnaire;
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

    public QuestionnaireItemType getItemType() {
        return itemType;
    }

    public void setItemType(QuestionnaireItemType itemType) {
        this.itemType = itemType;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isRepeats() {
        return repeats;
    }

    public void setRepeats(boolean repeats) {
        this.repeats = repeats;
    }

    public Integer getItemOrder() {
        return itemOrder;
    }

    public void setItemOrder(Integer itemOrder) {
        this.itemOrder = itemOrder;
    }

    public QuestionnaireItem getParentItem() {
        return parentItem;
    }

    public void setParentItem(QuestionnaireItem parentItem) {
        this.parentItem = parentItem;
    }

    public Set<QuestionnaireItem> getChildItems() {
        return childItems;
    }

    public void setChildItems(Set<QuestionnaireItem> childItems) {
        this.childItems = childItems;
    }

    public Set<QuestionnaireAnswerOption> getAnswerOptions() {
        return answerOptions;
    }

    public void setAnswerOptions(Set<QuestionnaireAnswerOption> answerOptions) {
        this.answerOptions = answerOptions;
    }

    public Set<QuestionnaireItemInitial> getInitials() {
        return initials;
    }

    public void setInitials(Set<QuestionnaireItemInitial> initials) {
        this.initials = initials;
    }

    public enum QuestionnaireItemType {

        GROUP("Group"), DISPLAY("Display"), BOOLEAN("Boolean"), DECIMAL("Decimal"), INTEGER("Integer"), DATE("Date"),
        DATETIME("Date Time"), TIME("Time"), STRING("String"), TEXT("Text"), URL("URL"), CHOICE("Choice"),
        OPEN_CHOICE("Open Choice"), ATTACHMENT("Attachment"), REFERENCE("Reference"), QUANTITY("Quantity");

        private final String display;

        QuestionnaireItemType(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
}