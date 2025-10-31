package org.openelisglobal.notebook.form;

import jakarta.validation.constraints.NotNull;
import java.util.Base64;
import java.util.List;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.openelisglobal.notebook.valueholder.NoteBookFile;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.validation.annotations.SafeHtml;

public class NoteBookForm {
    private Integer id;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String title;
    private Integer type;
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String project;
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String objective;
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String protocol;
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String content;
    private Integer technicianId;
    @NotNull
    private Integer patientId;
    private Integer systemUserId;
    private NoteBookStatus status;
    private List<Integer> sampleIds;
    private List<String> tags;
    private List<NoteBookPage> pages;
    private List<NoteBookFileForm> files;
    private List<Integer> analyzerIds;

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

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Integer technicianId) {
        this.technicianId = technicianId;
    }

    public List<Integer> getSampleIds() {
        return sampleIds;
    }

    public void setSampleIds(List<Integer> sampleIds) {
        this.sampleIds = sampleIds;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<NoteBookFileForm> getFiles() {
        return files;
    }

    public void setFiles(List<NoteBookFileForm> files) {
        this.files = files;
    }

    public Integer getPatientId() {
        return patientId;
    }

    public void setPatientId(Integer patientId) {
        this.patientId = patientId;
    }

    public List<NoteBookPage> getPages() {
        return pages;
    }

    public void setPages(List<NoteBookPage> pages) {
        this.pages = pages;
    }

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<Integer> getAnalyzerIds() {
        return analyzerIds;
    }

    public void setAnalyzerIds(List<Integer> analyzerIds) {
        this.analyzerIds = analyzerIds;
    }

    public NoteBookStatus getStatus() {
        return status;
    }

    public void setStatus(NoteBookStatus status) {
        this.status = status;
    }

    public static class NoteBookFileForm extends NoteBookFile {

        private static final long serialVersionUID = 3142138533368581327L;

        @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
        private String base64File;

        @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
        public String getBase64File() {
            return base64File;
        }

        public void setBase64File(String base64File) {
            this.base64File = base64File;
            String[] imageInfo = base64File.split(";base64,", 2);

            setFileType(imageInfo[0]);
            setFileData(Base64.getDecoder().decode(imageInfo[1]));
        }
    }

}
