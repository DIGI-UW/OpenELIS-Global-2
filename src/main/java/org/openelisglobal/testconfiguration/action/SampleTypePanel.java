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

package org.openelisglobal.testconfiguration.action;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.panel.valueholder.Panel;

public class SampleTypePanel {
    private String typeOfSampleName;
    private List<Panel> panels;

    public SampleTypePanel(String typeOfSample) {
        this.typeOfSampleName = typeOfSample;
        this.panels = new ArrayList<Panel>();
    }

    public String getTypeOfSampleName() {
        return typeOfSampleName;
    }

    public void setTypeOfSampleName(String typeOfSample) {
        this.typeOfSampleName = typeOfSample;
    }

    public List<Panel> getPanels() {
        return panels;
    }

    public void setPanels(List<Panel> panels) {
        this.panels = panels;
    }
}
