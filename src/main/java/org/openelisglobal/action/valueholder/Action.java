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

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.valueholder.BaseObject;

@Setter
@Getter
@Entity
@DynamicUpdate
@Table(name = "ACTION")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class Action extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "action_seq_gen")
    @GenericGenerator(name = "action_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = {
            @Parameter(name = "sequence_name", value = "action_seq") })
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0, nullable = false)
    private String id;

    @Column(name = "CODE", length = 10, nullable = false)
    private String code;

    @Column(name = "DESCRIPTION", length = 256, nullable = false)
    private String description;

    @Column(name = "TYPE", length = 10, nullable = false)
    private String type;

    // (concatenate action code name/desc)
    // Marked as transient because it's a computed property, not a database column
    @Transient
    private String actionDisplayValue;

    public Action() {
        super();
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
