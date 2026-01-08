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
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Date;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.validation.annotations.SafeHtml;

@Entity
@Table(name = "notebook_comment")
public class NoteBookComment extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notebook_comment_generator")
    @SequenceGenerator(name = "notebook_comment_generator", sequenceName = "notebook_comment_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "comment_text")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String text;

    @Column(name = "date_created")
    private Date dateCreated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_id", nullable = false)
    @JsonIgnore
    private NoteBook notebook;

    @OneToOne
    @JoinColumn(name = "system_user_id", referencedColumnName = "id")
    private SystemUser author;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
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

    public NoteBook getNotebook() {
        return notebook;
    }

    public void setNotebook(NoteBook notebook) {
        this.notebook = notebook;
    }

    public SystemUser getAuthor() {
        return author;
    }

    public void setAuthor(SystemUser author) {
        this.author = author;
    }

}
