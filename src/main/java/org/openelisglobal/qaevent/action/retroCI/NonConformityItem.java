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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.qaevent.action.retroCI;

import java.io.Serializable;

public class NonConformityItem implements Serializable {

    private static final long serialVersionUID = 77146028090504294L;
    private String qaEventId;
    private String qaName;
    private boolean violetTube;
    private boolean redTube;
    private boolean driedBloodSpot;
    private boolean simpleSelect;
    private String note;

    public String getQaEventId() {
        return qaEventId;
    }

    public void setQaEventId(String qaEventId) {
        this.qaEventId = qaEventId;
    }

    public void setQaName(String qaName) {
        this.qaName = qaName;
    }

    public String getQaName() {
        return qaName;
    }

    public boolean isVioletTube() {
        return violetTube;
    }

    public void setVioletTube(boolean violetTube) {
        this.violetTube = violetTube;
    }

    public boolean isRedTube() {
        return redTube;
    }

    public void setRedTube(boolean redTube) {
        this.redTube = redTube;
    }

    public boolean isDriedBloodSpot() {
        return driedBloodSpot;
    }

    public void setDriedBloodSpot(boolean driedBloodSpot) {
        this.driedBloodSpot = driedBloodSpot;
    }

    public void setSimpleSelect(boolean simpleSelect) {
        this.simpleSelect = simpleSelect;
    }

    public boolean isSimpleSelect() {
        return simpleSelect;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
