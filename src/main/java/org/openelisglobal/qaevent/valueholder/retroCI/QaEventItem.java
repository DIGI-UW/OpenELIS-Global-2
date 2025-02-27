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
 * Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
 *
 * Contributor(s): CIRG, University of Washington, Seattle WA.
 */
/** Value Object for transfer from UI table of QaEvents to Action classes(s). */
package org.openelisglobal.qaevent.valueholder.retroCI;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.groups.Default;
import java.io.Serializable;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.qaevent.form.NonConformityForm;
import org.openelisglobal.validation.annotations.SafeHtml;

public class QaEventItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @Pattern(regexp = "^NEW$|" + ValidationHelper.ID_REGEX, groups = { NonConformityForm.NonConformity.class })
    private String id;

    @NotBlank(groups = { Default.class })
    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { NonConformityForm.NonConformity.class, Default.class })
    private String qaEvent;

    @NotBlank(groups = { NonConformityForm.NonConformity.class, Default.class })
    @Pattern(regexp = "^-1$|" + ValidationHelper.ID_REGEX, groups = { NonConformityForm.NonConformity.class,
            Default.class })
    private String sampleType;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { NonConformityForm.NonConformity.class, Default.class })
    private String section;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { NonConformityForm.NonConformity.class, Default.class })
    private String authorizer;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { NonConformityForm.NonConformity.class, Default.class })
    private String note;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { NonConformityForm.NonConformity.class, Default.class })
    private String recordNumber;

    private boolean remove;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQaEvent() {
        return qaEvent;
    }

    public void setQaEvent(String qaEvent) {
        this.qaEvent = qaEvent;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getAuthorizer() {
        return authorizer;
    }

    public void setAuthorizer(String authorizer) {
        this.authorizer = authorizer;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String notes) {
        note = notes;
    }

    public boolean isRemove() {
        return remove;
    }

    public void setRemove(boolean remove) {
        this.remove = remove;
    }

    public String getRecordNumber() {
        return recordNumber;
    }

    public void setRecordNumber(String recordNumber) {
        this.recordNumber = recordNumber;
    }

    @Override
    public String toString() {
        return "QaEventValue [id=" + id + ", qaEvent=" + qaEvent + ", sampleType=" + sampleType + ", authorizer="
                + authorizer + ", note=" + note + ", remove=" + remove + ", section=" + section + "]";
    }
}
