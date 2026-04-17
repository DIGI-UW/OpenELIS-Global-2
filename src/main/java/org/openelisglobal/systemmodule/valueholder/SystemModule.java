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
package org.openelisglobal.systemmodule.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.converter.StringToIntegerConverter;

/**
 * @author Hung Nguyen (Hung.Nguyen@health.state.mn.us)
 */
@Entity
@Table(name = "system_module", schema = "clinlims")
public class SystemModule extends BaseObject<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "system_module_seq_gen")
    @SequenceGenerator(name = "system_module_seq_gen", sequenceName = "system_module_seq", schema = "clinlims", allocationSize = 1)
    @Convert(converter = StringToIntegerConverter.class)
    @Column(name = "id")
    private String id;

    @Column(name = "name", length = 32)
    private String systemModuleName;

    @Column(name = "description", length = 80)
    private String description;

    @Column(name = "has_select_flag", length = 1)
    private String hasSelectFlag;

    @Column(name = "has_add_flag", length = 1)
    private String hasAddFlag;

    @Column(name = "has_update_flag", length = 1)
    private String hasUpdateFlag;

    @Column(name = "has_delete_flag", length = 1)
    private String hasDeleteFlag;

    public SystemModule() {
        super();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setSystemModuleName(String systemModuleName) {
        this.systemModuleName = systemModuleName;
    }

    public String getSystemModuleName() {
        return systemModuleName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setHasSelectFlag(String hasSelectFlag) {
        this.hasSelectFlag = hasSelectFlag;
    }

    public String getHasSelectFlag() {
        return hasSelectFlag;
    }

    public void setHasAddFlag(String hasAddFlag) {
        this.hasAddFlag = hasAddFlag;
    }

    public String getHasAddFlag() {
        return hasAddFlag;
    }

    public void setHasUpdateFlag(String hasUpdateFlag) {
        this.hasUpdateFlag = hasUpdateFlag;
    }

    public String getHasUpdateFlag() {
        return hasUpdateFlag;
    }

    public void setHasDeleteFlag(String hasDeleteFlag) {
        this.hasDeleteFlag = hasDeleteFlag;
    }

    public String getHasDeleteFlag() {
        return hasDeleteFlag;
    }
}
