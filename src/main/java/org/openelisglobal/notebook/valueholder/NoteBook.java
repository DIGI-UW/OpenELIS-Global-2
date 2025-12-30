package org.openelisglobal.notebook.valueholder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.valueholder.TestSection;

@Entity
@Table(name = "notebook")
public class NoteBook extends BaseObject<Integer> {

    private static final long serialVersionUID = -979624722823577192L;

    public enum NoteBookStatus {
        DRAFT("Draft"), SUBMITTED("Submitted"), FINALIZED("Finalized"), LOCKED("Locked"), ARCHIVED("Archived"),
        ACTIVE("Active");

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

    @Column(name = "objective")
    private String objective;

    @Column(name = "protocol")
    private String protocol;

    // Project metadata fields - inherited by child instances from parent templates
    @Column(name = "principal_investigator")
    private String principalInvestigator;

    @Column(name = "funding_source")
    private String fundingSource;

    @Column(name = "budget", precision = 15, scale = 2)
    private BigDecimal budget;

    @Column(name = "project_timeline")
    private String projectTimeline;

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

    @Valid
    @OneToOne
    @JoinColumn(name = "creator_id", referencedColumnName = "id")
    private SystemUser creator;

    @OneToMany
    @JoinTable(name = "notebook_samples_list", joinColumns = @JoinColumn(name = "notebook_id"), inverseJoinColumns = @JoinColumn(name = "sample_item_id"))
    private List<SampleItem> samples = new ArrayList<>();

    @OneToMany
    @JoinTable(name = "notebook_analysers", joinColumns = @JoinColumn(name = "notebook_id"), inverseJoinColumns = @JoinColumn(name = "analyser_id"))
    private List<Analyzer> analysers = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "notebook_inventory_instruments", joinColumns = @JoinColumn(name = "notebook_id"))
    @Column(name = "inventory_item_id")
    private List<Long> inventoryInstrumentIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "notebook_tags", joinColumns = @JoinColumn(name = "notebook_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @OneToMany(mappedBy = "notebook", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("order ASC")
    private List<NoteBookPage> pages = new ArrayList<>();

    @OneToMany(mappedBy = "notebook", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NoteBookFile> files = new ArrayList<>();

    @OneToMany(mappedBy = "notebook", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NoteBookComment> comments = new ArrayList<>();

    @OneToMany
    @JoinTable(name = "notebook_entries", joinColumns = @JoinColumn(name = "notebook_id"), inverseJoinColumns = @JoinColumn(name = "entry_id"))
    private List<NoteBook> entries = new ArrayList<>();

    @Column(name = "questionnaire_fhir_uuid", columnDefinition = "uuid")
    private UUID questionnaireFhirUuid;

    @ManyToMany
    @JoinTable(name = "notebook_organizations", joinColumns = @JoinColumn(name = "notebook_id"), inverseJoinColumns = @JoinColumn(name = "organization_id"))
    private Set<Organization> organizations = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "notebook_departments", joinColumns = @JoinColumn(name = "notebook_id"), inverseJoinColumns = @JoinColumn(name = "test_section_id"))
    private Set<TestSection> departments = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "notebook_allowed_roles", joinColumns = @JoinColumn(name = "notebook_id"))
    @Column(name = "role")
    private Set<String> allowedRoles = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_notebook_id")
    private NoteBook parentNotebook;

    @OneToMany(mappedBy = "parentNotebook", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("title ASC")
    private List<NoteBook> childInstances = new ArrayList<>();

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

    public String getPrincipalInvestigator() {
        return principalInvestigator;
    }

    public void setPrincipalInvestigator(String principalInvestigator) {
        this.principalInvestigator = principalInvestigator;
    }

    public String getFundingSource() {
        return fundingSource;
    }

    public void setFundingSource(String fundingSource) {
        this.fundingSource = fundingSource;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public String getProjectTimeline() {
        return projectTimeline;
    }

    public void setProjectTimeline(String projectTimeline) {
        this.projectTimeline = projectTimeline;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<SampleItem> getSamples() {
        return samples;
    }

    public void setSamples(List<SampleItem> samples) {
        this.samples = samples;
    }

    public List<NoteBookPage> getPages() {
        return pages;
    }

    public void setPages(List<NoteBookPage> pages) {
        this.pages = pages;
    }

    public List<Analyzer> getAnalysers() {
        return analysers;
    }

    public void setAnalysers(List<Analyzer> analysers) {
        this.analysers = analysers;
    }

    public List<Long> getInventoryInstrumentIds() {
        if (inventoryInstrumentIds == null) {
            inventoryInstrumentIds = new ArrayList<>();
        }
        return inventoryInstrumentIds;
    }

    public void setInventoryInstrumentIds(List<Long> inventoryInstrumentIds) {
        this.inventoryInstrumentIds = inventoryInstrumentIds;
    }

    public List<String> getTags() {
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
        return files;
    }

    public void setFiles(List<NoteBookFile> files) {
        this.files = files;
    }

    public List<NoteBookComment> getComments() {
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

    public SystemUser getCreator() {
        return creator;
    }

    public void setCreator(SystemUser creator) {
        this.creator = creator;
    }

    public Set<Organization> getOrganizations() {
        if (organizations == null) {
            organizations = new HashSet<>();
        }
        return organizations;
    }

    public void setOrganizations(Set<Organization> organizations) {
        this.organizations = organizations;
    }

    public Set<TestSection> getDepartments() {
        if (departments == null) {
            departments = new HashSet<>();
        }
        return departments;
    }

    public void setDepartments(Set<TestSection> departments) {
        this.departments = departments;
    }

    public Set<String> getAllowedRoles() {
        if (allowedRoles == null) {
            allowedRoles = new HashSet<>();
        }
        return allowedRoles;
    }

    public void setAllowedRoles(Set<String> allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    public NoteBook getParentNotebook() {
        return parentNotebook;
    }

    public void setParentNotebook(NoteBook parentNotebook) {
        this.parentNotebook = parentNotebook;
    }

    public List<NoteBook> getChildInstances() {
        if (childInstances == null) {
            childInstances = new ArrayList<>();
        }
        return childInstances;
    }

    public void setChildInstances(List<NoteBook> childInstances) {
        this.childInstances = childInstances;
    }

    /**
     * Returns true if this is a parent template (can spawn children, cannot have
     * entries directly). A parent template has isTemplate=true and no parent
     * notebook.
     */
    public boolean isParentTemplate() {
        return Boolean.TRUE.equals(isTemplate) && parentNotebook == null;
    }

    /**
     * Returns true if this is a child instance (can have entries, cannot spawn
     * children). A child instance has isTemplate=false and has a parent notebook.
     */
    public boolean isChildInstance() {
        return !Boolean.TRUE.equals(isTemplate) && parentNotebook != null;
    }

    /**
     * Returns the effective pages for this notebook. Child instances inherit pages
     * from their parent template (live inheritance). Parent templates and
     * standalone notebooks return their own pages.
     */
    public List<NoteBookPage> getEffectivePages() {
        if (isChildInstance() && parentNotebook != null) {
            return parentNotebook.getPages();
        }
        return this.pages;
    }

    /**
     * Helper method to add a child instance to this parent template. Sets up the
     * bidirectional relationship.
     */
    public void addChildInstance(NoteBook child) {
        getChildInstances().add(child);
        child.setParentNotebook(this);
    }

    /**
     * Helper method to remove a child instance from this parent template. Clears
     * the bidirectional relationship.
     */
    public void removeChildInstance(NoteBook child) {
        getChildInstances().remove(child);
        child.setParentNotebook(null);
    }
}
