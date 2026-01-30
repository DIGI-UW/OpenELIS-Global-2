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
package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "analyzer")
public class Analyzer extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID", precision = 10, scale = 0)
    @GeneratedValue(generator = "analyzer_seq_gen")
    @GenericGenerator(name = "analyzer_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "analyzer_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @Column(name = "script_id", length = 20)
    private String script_id;

    @Column(name = "name", length = 20)
    private String name;

    @Column(name = "machine_id", length = 20)
    private String machineId;

    @Column(name = "analyzer_type", length = 30)
    private String type;

    @Column(name = "description", length = 60)
    private String description;

    @Column(name = "location", length = 60)
    private String location;

    @Column(name = "is_active", length = 1)
    private boolean active;

    @Column(name = "has_setup_page", length = 1)
    private boolean hasSetupPage;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getScript_id() {
        return script_id;
    }

    public void setScript_id(String script_id) {
        this.script_id = script_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getMachineId() {
        return machineId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean getHasSetupPage() {
        return hasSetupPage;
    }

    public void setHasSetupPage(boolean hasSetupPage) {
        this.hasSetupPage = hasSetupPage;
    }
}
