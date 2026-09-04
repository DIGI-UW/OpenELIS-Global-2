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
package org.openelisglobal.label.valueholder;

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
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.scriptlet.valueholder.Scriptlet;

@Setter
@Getter
@DynamicUpdate
@Entity
@Table(name = "label")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class Label extends EnumValueItemImpl {

    @Id
    @GeneratedValue(generator = "label_seq_gen")
    @GenericGenerator(name = "label_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = {
            @Parameter(name = "sequence_name", value = "label_seq") })
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0, nullable = false)
    private String id;

    @Column(name = "name", length = 30)
    private String labelName;

    @Column(name = "description", length = 60)
    private String description;

    @Column(name = "printer_type", length = 1)
    private String printerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scriptlet_id")
    private Scriptlet scriptlet;

    @Transient
    private String scriptletName;

    public Label() {
        super();
    }

    protected Scriptlet getScriptletHolder() {
        return this.scriptlet;
    }

    protected void setScriptletHolder(Scriptlet scriptlet) {
        this.scriptlet = scriptlet;
    }

}
