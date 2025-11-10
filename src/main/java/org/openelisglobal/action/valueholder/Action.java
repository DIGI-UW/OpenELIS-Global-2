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
package org.openelisglobal.action.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "action")
public class Action extends BaseObject<String> {

    @Id
    @GenericGenerator(name = "action_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "action_seq"))
    @GeneratedValue(generator = "action_seq_gen ")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;

    @Column(name = "CODE", nullable = true, length = 10)
    private String code;
    @Column(name = "DESCRIPTION", nullable = false, length = 256)
    private String description;
    @Column(name = "TYPE", nullable = false, length = 10)
    private String type;

    // (concatenate action code name/desc)
    private String actionDisplayValue;

    public Action() {
        super();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getActionDisplayValue() {
        if (!StringUtil.isNullorNill(this.code)) {
            actionDisplayValue = code + "-" + description;
        } else {
            actionDisplayValue = description;
        }
        return actionDisplayValue;
    }
}
