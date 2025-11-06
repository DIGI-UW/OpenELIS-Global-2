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

package org.openelisglobal.test.beanItems;

import java.io.Serializable;
import java.util.List;
import org.openelisglobal.common.util.IdValuePair;

public class TestActivationBean implements Serializable {
    private IdValuePair sampleType;
    private List<IdValuePair> activeTests;
    private List<IdValuePair> inactiveTests;

    public IdValuePair getSampleType() {
        return sampleType;
    }

    public void setSampleType(IdValuePair sampleType) {
        this.sampleType = sampleType;
    }

    public List<IdValuePair> getActiveTests() {
        return activeTests;
    }

    public void setActiveTests(List<IdValuePair> activeTests) {
        this.activeTests = activeTests;
    }

    public List<IdValuePair> getInactiveTests() {
        return inactiveTests;
    }

    public void setInactiveTests(List<IdValuePair> inactiveTests) {
        this.inactiveTests = inactiveTests;
    }
}
