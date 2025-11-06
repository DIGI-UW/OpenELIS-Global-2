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
package org.openelisglobal.region.valueholder;

import java.util.HashSet;
import java.util.Set;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;

public class Region extends EnumValueItemImpl {

    private String region;

    private String id;

    private Set counties = new HashSet(0);

    public Region() {
        super();
    }

    public String getRegion() {
        return this.region;
    }

    public String getId() {
        return this.id;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set getCounties() {
        return this.counties;
    }

    public void setCounties(Set counties) {
        this.counties = counties;
    }
}
