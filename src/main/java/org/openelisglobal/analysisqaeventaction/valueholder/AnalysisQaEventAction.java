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
 */
package org.openelisglobal.analysisqaeventaction.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.sql.Date;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.action.valueholder.Action;
import org.openelisglobal.analysisqaevent.valueholder.AnalysisQaEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@Setter
@Getter
@DynamicUpdate
@Entity
@Table(name = "ANALYSIS_QAEVENT_ACTION")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class AnalysisQaEventAction extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "analysis_qaevent_action_seq")
    @GenericGenerator(name = "analysis_qaevent_action_seq", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = {
            @Parameter(name = "sequence_name", value = "analysis_qaevent_action_seq") })
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ANALYSIS_QAEVENT_ID", nullable = false)
    private AnalysisQaEvent analysisQaEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACTION_ID", nullable = false)
    private Action action;

    @Column(name = "CREATED_DATE", length = 7, nullable = false)
    private Date createdDate;

    @Transient
    private String createdDateForDisplay;

    // bugzilla 2481
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SYS_USER_ID")
    private SystemUser systemUser;

    @Transient
    private String systemUserId;

    public AnalysisQaEventAction() {
        super();
    }

    protected Action getActionHolder() {
        return this.action;
    }

    protected void setActionHolder(Action action) {
        this.action = action;
    }

    protected AnalysisQaEvent getAnalysisQaEventHolder() {
        return this.analysisQaEvent;
    }

    protected void setAnalysisQaEventHolder(AnalysisQaEvent analysisQaEvent) {
        this.analysisQaEvent = analysisQaEvent;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
        this.createdDateForDisplay = DateUtil.convertSqlDateToStringDate(createdDate);
    }

    public void setCreatedDateForDisplay(String createdDateForDisplay) {
        this.createdDateForDisplay = createdDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        this.createdDate = DateUtil.convertStringDateToSqlDate(createdDateForDisplay, locale);
    }
}
