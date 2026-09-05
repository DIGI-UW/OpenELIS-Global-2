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
package org.openelisglobal.method.valueholder;

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
import org.hibernate.annotations.Type;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.localization.valueholder.Localization;

@Getter
@Setter
@DynamicUpdate
@Entity
@Table(name = "method")
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class Method extends EnumValueItemImpl {

    @Id
    @GeneratedValue(generator = "method_seq_gen")
    @GenericGenerator(name = "method_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = {
            @org.hibernate.annotations.Parameter(name = "sequence_name", value = "method_seq") })
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;

    @Column(name = "NAME", length = 20, nullable = false)
    private String methodName;

    @Column(name = "DESCRIPTION", length = 60, nullable = false)
    private String description;

    @Column(name = "REPORTING_DESCRIPTION", length = 60)
    private String reportingDescription;

    @Column(name = "ACTIVE_BEGIN", length = 7)
    private Date activeBeginDate = null;

    @Transient
    private String activeBeginDateForDisplay = null;

    @Column(name = "ACTIVE_END", length = 7)
    private Date activeEndDate = null;

    @Transient
    private String activeEndDateForDisplay = null;

    @Column(name = "IS_ACTIVE", length = 1)
    private String isActive;

    @Column(name = "CODE", length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "name_localization_id")
    private Localization localization;

    public Method() {
        super();
    }

    public void setActiveBeginDate(Date activeBeginDate) {
        this.activeBeginDate = activeBeginDate;
        this.activeBeginDateForDisplay = DateUtil.convertSqlDateToStringDate(activeBeginDate);
    }

    public void setActiveEndDate(Date activeEndDate) {
        this.activeEndDate = activeEndDate;
        this.activeEndDateForDisplay = DateUtil.convertSqlDateToStringDate(activeEndDate);
    }

    public void setActiveBeginDateForDisplay(String activeBeginDateForDisplay) {
        this.activeBeginDateForDisplay = activeBeginDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        this.activeBeginDate = DateUtil.convertStringDateToSqlDate(this.activeBeginDateForDisplay, locale);
    }

    public void setActiveEndDateForDisplay(String activeEndDateForDisplay) {
        this.activeEndDateForDisplay = activeEndDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        this.activeEndDate = DateUtil.convertStringDateToSqlDate(activeEndDateForDisplay, locale);
    }

    public String getLocalizedValue() {
        if (getLocalization() == null) {
            return methodName;
        } else {
            return getLocalization().getLocalizedValue();
        }
    }
}
