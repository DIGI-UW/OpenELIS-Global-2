package org.openelisglobal.notebook.bean;

import java.util.List;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.notebook.valueholder.NoteBookComment;
import org.openelisglobal.notebook.valueholder.NoteBookFile;
import org.openelisglobal.notebook.valueholder.NoteBookPage;

public class NoteBookFullDisplayBean extends NoteBookDisplayBean {

    private String protocol;
    private String objective;
    private List<IdValuePair> analyzers;
    private String project;
    private String content;
    private List<NoteBookPage> pages;
    private List<NoteBookFile> files;
    private List<NoteBookComment> comments;
    private List<SampleDisplayBean> samples;
    private Integer technicianId;
    private String technicianName;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<NoteBookPage> getPages() {
        return pages;
    }

    public void setPages(List<NoteBookPage> pages) {
        this.pages = pages;
    }

    public List<NoteBookFile> getFiles() {
        return files;
    }

    public void setFiles(List<NoteBookFile> files) {
        this.files = files;
    }

    public Integer getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Integer technicianId) {
        this.technicianId = technicianId;
    }

    public List<SampleDisplayBean> getSamples() {
        return samples;
    }

    public void setSamples(List<SampleDisplayBean> samples) {
        this.samples = samples;
    }

    public String getTechnicianName() {
        return technicianName;
    }

    public void setTechnicianName(String technicianName) {
        this.technicianName = technicianName;
    }

    public List<IdValuePair> getAnalyzers() {
        return analyzers;
    }

    public void setAnalyzers(List<IdValuePair> analyzers) {
        this.analyzers = analyzers;
    }

    public List<NoteBookComment> getComments() {
        return comments;
    }

    public void setComments(List<NoteBookComment> comments) {
        this.comments = comments;
    }
}
