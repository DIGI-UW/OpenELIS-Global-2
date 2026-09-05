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
package org.openelisglobal.note.valueholder;

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
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@Setter
@Getter
@DynamicUpdate
@Entity
@Table(name = "NOTE")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class Note extends BaseObject<String> {
    public static final String EXTERNAL = "E";
    public static final String INTERNAL = "I";
    public static final String REJECT_REASON = "R";
    public static final String NON_CONFORMITY = "N";
    public static final String UNCONDITIONAL_ACCEPTANCE_REASON = "U";

    @Id
    @GeneratedValue(generator = "note_seq_gen")
    @GenericGenerator(name = "note_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = {
            @org.hibernate.annotations.Parameter(name = "sequence_name", value = "note_seq") })
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SYS_USER_ID")
    private SystemUser systemUser;

    @Transient
    private String systemUserId;

    @Column(name = "REFERENCE_ID")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String referenceId;

    @Column(name = "REFERENCE_TABLE")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String referenceTableId;

    @Column(name = "NOTE_TYPE", length = 1)
    private String noteType;

    @Column(name = "SUBJECT", length = 60)
    private String subject;

    @Column(name = "TEXT")
    private String text;

    /**
     * OGC-811 (Results Entry v3) — scopes a note to one result component of a
     * multi-component test. Null means analysis-level (legacy notes and notes
     * authored outside a component context), which displays on every component row.
     */
    @Column(name = "TEST_RESULT_COMPONENT_ID", length = 36)
    private String testResultComponentId;

    public void setReferenceTables(ReferenceTables referenceTables) {
        if (referenceTables != null) {
            setReferenceTableId(referenceTables.getId());
        }
    }
}
