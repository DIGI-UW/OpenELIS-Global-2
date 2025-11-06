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
package org.openelisglobal.county.valueholder;

import java.util.HashSet;
import java.util.Set;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.region.valueholder.Region;

public class County extends EnumValueItemImpl {

    private String id;

    private String county;

    private String regionId;

    private ValueHolderInterface region;

    private Set cities = new HashSet(0);

    public County() {
        super();
        this.region = new ValueHolder();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getCounty() {
        return county;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getRegionId() {
        return regionId;
    }

    public Region getRegion() {
        return (Region) this.region.getValue();
    }

    protected ValueHolderInterface getRegionHolder() {
        return this.region;
    }

    public void setRegion(Region region) {
        this.region.setValue(region);
    }

    protected void setRegionHolder(ValueHolderInterface region) {
        this.region = region;
    }
}
