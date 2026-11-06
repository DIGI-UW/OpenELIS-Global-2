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
package org.openelisglobal.sampleproject.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.sample.valueholder.Sample;

@Setter
@Getter
@DynamicUpdate
@Entity
@Table(name = "SAMPLE_PROJECTS")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class SampleProject extends BaseObject<String> {

    /** */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "sample_proj_seq_gen")
    @GenericGenerator(name = "sample_proj_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "sample_proj_seq"))
    @Column(name = "ID", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROJ_ID", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SAMP_ID", nullable = false)
    private Sample sample;

    @Column(name = "IS_PERMANENT", length = 10)
    private String isPermanent;

    public SampleProject() {
        super();
    }

    protected Sample getSampleHolder() {
        return this.sample;
    }

    protected void setSampleHolder(Sample sample) {
        this.sample = sample;
    }
}
