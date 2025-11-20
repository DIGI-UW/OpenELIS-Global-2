package org.openelisglobal.notebook.valueholder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@Entity
@Table(name = "notebook")
public class NoteBook extends BaseObject<Integer> {

    private static final long serialVersionUID = -979624722823577192L;

    public enum NoteBookStatus {
        DRAFT("Draft"), SUBMITTED("Submitted"), FINALIZED("Finalized"), LOCKED("Locked"), ARCHIVED("Archived");

        private String display;

        NoteBookStatus(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notebook_generator")
    @SequenceGenerator(name = "notebook_generator", sequenceName = "notebook_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "is_template")
    private Boolean isTemplate;

    @Column(name = "title")
    private String title;

    @ManyToOne
    @JoinColumn(name = "type", referencedColumnName = "id")
    private Dictionary type;

    @Column(name = "project")
    private String project;

    @Column(name = "objective")
    private String objective;

    @Column(name = "protocol")
    private String protocol;

    @Column(name = "content")
    private String content;

    @Column(name = "date_created")
    private Date dateCreated;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "status")
    private NoteBookStatus status = NoteBookStatus.DRAFT;

    @Valid
    @OneToOne
    @JoinColumn(name = "technician_id", referencedColumnName = "id")
    private SystemUser technician;

    @OneToMany
    @JoinTable(name = "notebook_samples", joinColumns = @JoinColumn(name = "notebook_id"), inverseJoinColumns = @JoinColumn(name = "sample_item_id"))
    private List<SampleItem> samples;

    @OneToMany
    @JoinTable(name = "notebook_analysers", joinColumns = @JoinColumn(name = "notebook_id"), inverseJoinColumns = @JoinColumn(name = "analyser_id"))
    private List<Analyzer> analysers;

    @ElementCollection
    @CollectionTable(name = "notebook_tags", joinColumns = @JoinColumn(name = "notebook_id"))
    @Column(name = "tag")
    private List<String> tags;

    @OneToMany(mappedBy = "notebook", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NoteBookPage> pages;

    @OneToMany(mappedBy = "notebook", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NoteBookFile> files;

    @OneToMany(mappedBy = "notebook", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NoteBookComment> comments;

    @OneToMany
    @JoinTable(name = "notebook_entries", joinColumns = @JoinColumn(name = "notebook_id"), inverseJoinColumns = @JoinColumn(name = "entry_id"))
    private List<NoteBook> entries;
    @Column(name = "questionnaire_fhir_uuid")
    private UUID questionnaireFhirUuid;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Dictionary getType() {
        return type;
    }

    public void setType(Dictionary type) {
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

    public List<SampleItem> getSamples() {
        if (samples == null) {
            samples = new ArrayList<>();
        }
        return samples;
    }

    public void setSamples(List<SampleItem> samples) {
        this.samples = samples;
    }

    public List<NoteBookPage> getPages() {
        if (pages == null) {
            pages = new ArrayList<>();
        }
        return pages;
    }

    public void setPages(List<NoteBookPage> pages) {
        this.pages = pages;
    }

    public List<Analyzer> getAnalysers() {
        if (analysers == null) {
            analysers = new ArrayList<>();
        }
        return analysers;
    }

    public void setAnalysers(List<Analyzer> analysers) {
        this.analysers = analysers;
    }

    public List<String> getTags() {
        if (tags == null) {
            tags = new ArrayList<>();
        }
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public SystemUser getTechnician() {
        return technician;
    }

    public void setTechnician(SystemUser technician) {
        this.technician = technician;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public List<NoteBookFile> getFiles() {
        if (files == null) {
            files = new ArrayList<>();
        }
        return files;
    }

    public void setFiles(List<NoteBookFile> files) {
        this.files = files;
    }

    public List<NoteBookComment> getComments() {
        if (comments == null) {
            comments = new ArrayList<>();
        }
        return comments;
    }

    public void setComments(List<NoteBookComment> comments) {
        this.comments = comments;
    }

    public NoteBookStatus getStatus() {
        return status;
    }

    public void setStatus(NoteBookStatus status) {
        this.status = status;
    }

    public List<NoteBook> getEntries() {
        if (entries == null) {
            entries = new ArrayList<>();
        }
        return entries;
    }

    public void setEntries(List<NoteBook> entries) {
        this.entries = entries;
    }

    public Boolean getIsTemplate() {
        return isTemplate;
    }

    public void setIsTemplate(Boolean isTemplate) {
        this.isTemplate = isTemplate;
    }

    public UUID getQuestionnaireFhirUuid() {
        return questionnaireFhirUuid;
    }

    public void setQuestionnaireFhirUuid(UUID questionnaireFhirUuid) {
        this.questionnaireFhirUuid = questionnaireFhirUuid;
    }

}
