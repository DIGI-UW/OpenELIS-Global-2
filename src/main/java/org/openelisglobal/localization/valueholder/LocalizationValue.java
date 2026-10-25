/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.localization.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Represents a single translation value for a specific locale. Each
 * Localization entry can have multiple LocalizationValue entries, one per
 * supported language.
 */
@Entity
@Table(name = "localization_value")
@DynamicUpdate
public class LocalizationValue extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "localization_value_seq_gen")
    @GenericGenerator(name = "localization_value_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "localization_value_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "localization_id", nullable = false)
    private Localization localization;

    @Column(name = "locale", nullable = false)
    private String locale;

    @Column(name = "value", nullable = false)
    private String value;

    /**
     * Who wrote the label (OGC-1507): {@link #ORIGIN_SITE} for a lab's own edit in
     * the localization editor, {@link #ORIGIN_SYSTEM} for one OpenELIS wrote (a
     * configuration file, an upgrade backfill). On a value that ships with a
     * display key only a site label wins over the key's translation.
     */
    @Column(name = "origin", nullable = false)
    private String origin = ORIGIN_SITE;

    /**
     * Under a lab's own label, the label OpenELIS wrote for this language (the
     * configuration file's translation), so Reset has it to return to.
     */
    @Column(name = "system_value")
    private String systemValue;

    public static final String ORIGIN_SITE = "SITE";
    public static final String ORIGIN_SYSTEM = "SYSTEM";

    public LocalizationValue() {
    }

    public LocalizationValue(String locale, String value) {
        this.locale = locale;
        this.value = value;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Localization getLocalization() {
        return localization;
    }

    public void setLocalization(Localization localization) {
        this.localization = localization;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public boolean isSiteLabel() {
        return !ORIGIN_SYSTEM.equals(origin);
    }

    public String getSystemValue() {
        return systemValue;
    }

    public void setSystemValue(String systemValue) {
        this.systemValue = systemValue;
    }

    /** The label OpenELIS wrote for this language, or null. */
    public String getShippedValue() {
        return isSiteLabel() ? systemValue : value;
    }

    /** The lab writes its own label; the one OpenELIS wrote is kept. */
    public void writeSiteLabel(String newValue) {
        if (!isSiteLabel()) {
            systemValue = value;
        }
        value = newValue;
        origin = ORIGIN_SITE;
    }

    /**
     * OpenELIS writes its label. With keepSiteLabel a different lab label stays and
     * only the kept copy is updated.
     */
    public void writeSystemLabel(String newValue, boolean keepSiteLabel) {
        if (keepSiteLabel && isSiteLabel() && value != null && !value.equals(newValue)) {
            systemValue = newValue;
            return;
        }
        value = newValue;
        origin = ORIGIN_SYSTEM;
        systemValue = null;
    }
}
