package org.openelisglobal.notebook.valueholder;

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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * NotebookEntry - Represents an instance of lab work based on a notebook
 * template.
 *
 * <p>
 * This entity holds the actual work data (samples, status, comments) while
 * referencing the parent NoteBook (template) for shared metadata like pages,
 * protocol, and workflow structure.
 */
@Entity
@Table(name = "notebook_entry")
public class NotebookEntry extends BaseObject<Integer> {

    public enum EntryStatus {
        DRAFT, SUBMITTED, FINALIZED, LOCKED, ARCHIVED
    }

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notebook_entry_generator")
    @SequenceGenerator(name = "notebook_entry_generator", sequenceName = "notebook_entry_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_id", nullable = false)
    private NoteBook notebook;

    @Column(name = "title")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EntryStatus status = EntryStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private SystemUser technician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private SystemUser creator;

    @Column(name = "date_created", nullable = false)
    private Date dateCreated;

    @Column(name = "date_completed")
    private Date dateCompleted;

    @Column(name = "notes")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String notes;

    @Column(name = "manifest_description")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String manifestDescription;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "notebook_entry_sample", joinColumns = @JoinColumn(name = "notebook_entry_id"), inverseJoinColumns = @JoinColumn(name = "sample_item_id"))
    private List<SampleItem> samples = new ArrayList<>();

    @OneToMany(mappedBy = "notebookEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<NotebookEntryComment> comments = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", updatable = false)
    private Organization organization;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "notebook_entry_organizations", joinColumns = @JoinColumn(name = "notebook_entry_id"), inverseJoinColumns = @JoinColumn(name = "organization_id"))
    private Set<Organization> accessibleOrganizations = new HashSet<>();

    public NotebookEntry() {
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

    public NoteBook getNotebook() {
        return notebook;
    }

    public void setNotebook(NoteBook notebook) {
        this.notebook = notebook;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get the effective title - entry title if set, otherwise notebook template
     * title.
     */
    public String getEffectiveTitle() {
        if (title != null && !title.isEmpty()) {
            return title;
        }
        return notebook != null ? notebook.getTitle() : null;
    }

    public EntryStatus getStatus() {
        return status;
    }

    public void setStatus(EntryStatus status) {
        this.status = status;
    }

    public SystemUser getTechnician() {
        return technician;
    }

    public void setTechnician(SystemUser technician) {
        this.technician = technician;
    }

    public SystemUser getCreator() {
        return creator;
    }

    public void setCreator(SystemUser creator) {
        this.creator = creator;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(Date dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getManifestDescription() {
        return manifestDescription;
    }

    public void setManifestDescription(String manifestDescription) {
        this.manifestDescription = manifestDescription;
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

    public List<NotebookEntryComment> getComments() {
        if (comments == null) {
            comments = new ArrayList<>();
        }
        return comments;
    }

    public void setComments(List<NotebookEntryComment> comments) {
        this.comments = comments;
    }

    /** Add a sample to this entry. */
    public void addSample(SampleItem sample) {
        getSamples().add(sample);
    }

    /** Remove a sample from this entry. */
    public void removeSample(SampleItem sample) {
        getSamples().remove(sample);
    }

    /** Add a comment to this entry. */
    public void addComment(NotebookEntryComment comment) {
        comment.setNotebookEntry(this);
        getComments().add(comment);
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Set<Organization> getAccessibleOrganizations() {
        if (accessibleOrganizations == null) {
            accessibleOrganizations = new HashSet<>();
        }
        return accessibleOrganizations;
    }

    public void setAccessibleOrganizations(Set<Organization> accessibleOrganizations) {
        this.accessibleOrganizations = accessibleOrganizations;
    }

    /**
     * Get departments linked to this entry's notebook template. Used for
     * department-based sample type validation.
     */
    public Set<TestSection> getLinkedDepartments() {
        if (notebook != null) {
            return notebook.getDepartments();
        }
        return Collections.emptySet();
    }
}
