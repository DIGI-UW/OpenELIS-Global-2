package org.openelisglobal.notebook.bean;

import java.util.List;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;

public class NoteBookDisplayBean {
    private Integer id;
    private String title;
    private Integer type;
    private String lastName;
    private String firstName;
    private String gender;
    private String dateCreated;
    private List<String> tags;
    private String typeName;
    private NoteBookStatus status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public NoteBookStatus getStatus() {
        return status;
    }

    public void setStatus(NoteBookStatus status) {
        this.status = status;
    }

}
