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
import java.util.HashMap;
import java.util.List;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PanelTestConfigurationUtil {

    @Autowired
    TypeOfSamplePanelService typeOfSamplePanelService;
    @Autowired
    PanelService panelService;
    @Autowired
    TypeOfSampleService typeOfSampleService;

    @SuppressWarnings("unchecked")
    public HashMap<String, List<Panel>> createTypeOfSamplePanelMap(boolean isActive) {
        HashMap<String, List<Panel>> sampleTypeMap = new HashMap<>();

        List<TypeOfSamplePanel> listOfTypeOfSamplePanels = typeOfSamplePanelService.getAllTypeOfSamplePanels();
        for (TypeOfSamplePanel typeOfSamplePanel : listOfTypeOfSamplePanels) {
            TypeOfSample typeOfSample = typeOfSampleService.getTypeOfSampleById(typeOfSamplePanel.getTypeOfSampleId());
            List<Panel> panelsForThisSampleType = sampleTypeMap.get(typeOfSample.getLocalizedName());
            if (panelsForThisSampleType == null) {
                panelsForThisSampleType = new ArrayList<>();
                sampleTypeMap.put(typeOfSample.getLocalizedName(), panelsForThisSampleType);
            }
            Panel panel = panelService.getPanelById(typeOfSamplePanel.getPanelId());

            if ("Y".equals(panel.getIsActive()) && isActive) {
                sampleTypeMap.get(typeOfSample.getLocalizedName()).add(panel);
            } else if (!"Y".equals(panel.getIsActive()) && !isActive) {
                sampleTypeMap.get(typeOfSample.getLocalizedName()).add(panel);
            }
        }

        return sampleTypeMap;
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, List<Panel>> createTypeOfSamplePanelMap() {
        HashMap<String, List<Panel>> sampleTypeMap = new HashMap<>();

        List<TypeOfSamplePanel> listOfTypeOfSamplePanels = typeOfSamplePanelService.getAllTypeOfSamplePanels();
        for (TypeOfSamplePanel typeOfSamplePanel : listOfTypeOfSamplePanels) {
            TypeOfSample typeOfSample = typeOfSampleService.getTypeOfSampleById(typeOfSamplePanel.getTypeOfSampleId());
            List<Panel> panelsForThisSampleType = sampleTypeMap.get(typeOfSample.getLocalizedName());
            if (panelsForThisSampleType == null) {
                panelsForThisSampleType = new ArrayList<>();
                sampleTypeMap.put(typeOfSample.getLocalizedName(), panelsForThisSampleType);
            }
            Panel panel = panelService.getPanelById(typeOfSamplePanel.getPanelId());

            sampleTypeMap.get(typeOfSample.getLocalizedName()).add(panel);
        }

        return sampleTypeMap;
    }
}
