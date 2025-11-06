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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.result.action.util;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface ResultItem {

    public String getAccessionNumber();

    public void setAccessionNumber(String accessionNumber);

    public String getSequenceNumber();

    public void setSequenceNumber(String sequenceNumber);

    @JsonIgnore()
    public String getSequenceAccessionNumber();

    public void setShowSampleDetails(boolean showSampleDetails);

    public boolean isShowSampleDetails();

    public void setIsGroupSeparator(boolean isGroupSeparator);

    public boolean getIsGroupSeparator();

    public void setSampleGroupingNumber(int sampleGroupingNumber);

    public int getSampleGroupingNumber();

    public String getTestSortOrder();

    public String getTestName();
}
