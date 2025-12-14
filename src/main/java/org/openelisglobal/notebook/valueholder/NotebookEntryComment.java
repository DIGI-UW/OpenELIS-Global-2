package org.openelisglobal.notebook.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import java.util.Date;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * NotebookEntryComment - Comments specific to a notebook entry (instance).
 */
@Entity
@Table(name = "notebook_entry_comment")
public class NotebookEntryComment extends BaseObject<Integer> {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notebook_entry_comment_generator")
    @SequenceGenerator(name = "notebook_entry_comment_generator", sequenceName = "notebook_entry_comment_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_entry_id", nullable = false)
    @JsonIgnore
    private NotebookEntry notebookEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private SystemUser author;

    @Column(name = "text", nullable = false)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String text;

    @Column(name = "date_created", nullable = false)
    private Date dateCreated;

    public NotebookEntryComment() {
        this.dateCreated = new Date();
    }

    public NotebookEntryComment(String text, SystemUser author) {
        this.text = text;
        this.author = author;
        this.dateCreated = new Date();
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public NotebookEntry getNotebookEntry() {
        return notebookEntry;
    }

    public void setNotebookEntry(NotebookEntry notebookEntry) {
        this.notebookEntry = notebookEntry;
    }

    public SystemUser getAuthor() {
        return author;
    }

    public void setAuthor(SystemUser author) {
        this.author = author;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }
}
