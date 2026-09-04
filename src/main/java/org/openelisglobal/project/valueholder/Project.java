/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.project.valueholder;

import jakarta.persistence.*;
import java.sql.Date;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.scriptlet.valueholder.Scriptlet;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@Setter
@Getter
@Entity
@Table(name = "PROJECT")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class Project extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "project_seq_gen")
    @GenericGenerator(name = "project_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "project_seq"))
    @Column(name = "ID", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @Column(name = "NAME", length = 20, nullable = false)
    private String projectName;

    @Column(name = "DESCRIPTION", length = 60)
    private String description;

    @Transient
    private String stickerReqFlag;

    @Transient
    private String rptResultsFlag;

    @Transient
    private String printOnMailerFlag;

    @Column(name = "STARTED_DATE", length = 7)
    private Date startedDate = null;

    @Transient
    private String startedDateForDisplay = null;

    @Column(name = "COMPLETED_DATE", length = 7)
    private Date completedDate = null;

    @Transient
    private String completedDateForDisplay = null;

    @Column(name = "IS_ACTIVE", length = 1)
    private String isActive;

    @Column(name = "REFERENCE_TO", length = 20)
    private String referenceTo;

    @Column(name = "PROGRAM_CODE", length = 10)
    private String programCode;

    @Transient
    private String opOpId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SYS_USER_ID", nullable = false)
    private SystemUser systemUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SCRIPTLET_ID")
    private Scriptlet scriptlet;

    @Transient
    private String scriptletName;

    // AIS - bugzilla 1851
    @Transient
    private String concatProjNameDesc;

    // bugzilla 2438
    @Column(name = "LOCAL_ABBREV", length = 10, unique = true)
    private String localAbbreviation;

    /** All organization defined as associated with this project. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "project_organization", joinColumns = @JoinColumn(name = "project_id"), inverseJoinColumns = @JoinColumn(name = "org_id"))
    private Set<Organization> organizations;

    @Column(name = "display_key", length = 60)
    private String nameKey;

    @Override
    public String getNameKey() {
        return nameKey;
    }

    @Override
    public void setNameKey(String nameKey) {
        this.nameKey = nameKey;
    }

    public String getConcatProjNameDesc() {
        if (null == this.description) {
            return this.projectName;
        } else {
            return this.projectName + "+" + this.description;
        }
    }

    public Project() {
        super();
    }

    public void setStartedDate(Date startedDate) {
        this.startedDate = startedDate;
        this.startedDateForDisplay = DateUtil.convertSqlDateToStringDate(startedDate);
    }

    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate;
        this.completedDateForDisplay = DateUtil.convertSqlDateToStringDate(completedDate);
    }

    protected SystemUser getSystemUserHolder() {
        return this.systemUser;
    }

    protected void setSystemUserHolder(SystemUser systemUser) {
        this.systemUser = systemUser;
    }

    public void setStartedDateForDisplay(String startedDateForDisplay) {
        this.startedDateForDisplay = startedDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        this.startedDate = DateUtil.convertStringDateToSqlDate(this.startedDateForDisplay, locale);
    }

    public void setCompletedDateForDisplay(String completedDateForDisplay) {
        this.completedDateForDisplay = completedDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        this.completedDate = DateUtil.convertStringDateToSqlDate(completedDateForDisplay, locale);
    }

    protected Scriptlet getScriptletHolder() {
        return this.scriptlet;
    }

    protected void setScriptletHolder(Scriptlet scriptlet) {
        this.scriptlet = scriptlet;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return projectName;
    }

}
