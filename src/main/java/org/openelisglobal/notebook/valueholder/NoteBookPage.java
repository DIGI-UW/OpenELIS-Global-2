package org.openelisglobal.notebook.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.validation.annotations.SafeHtml;

@Entity
@Table(name = "notebook_page")
public class NoteBookPage extends BaseObject<Integer> {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notebook_page_generator")
    @SequenceGenerator(name = "notebook_page_generator", sequenceName = "notebook_page_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "page_order")
    private Integer order;

    @Column(name = "title")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String title;

    @Column(name = "instructions")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String instructions;

    @Column(name = "content")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_id", nullable = false)
    @JsonIgnore
    private NoteBook notebook;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notebook_page_tests", joinColumns = @JoinColumn(name = "notebook_page_id"))
    @Column(name = "test")
    private List<Integer> tests;

    @Column(name = "completed")
    private Boolean completed;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public NoteBook getNotebook() {
        return notebook;
    }

    public void setNotebook(NoteBook notebook) {
        this.notebook = notebook;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public List<Integer> getTests() {
        if (tests == null) {
            tests = new ArrayList<>();
        }
        return tests;
    }

    public void setTests(List<Integer> tests) {
        this.tests = tests;
    }

    public Boolean getCompleted() {
        return completed != null ? completed : false;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

}
