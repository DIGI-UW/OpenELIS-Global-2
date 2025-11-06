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
package org.openelisglobal.dataexchange.resultreporting.beans;

public class ResultXmit {

    private String loinc;

    public String getLoinc() {
        return loinc;
    }

    public void setLoinc(String loinc) {
        this.loinc = loinc;
    }

    /*
     * should be one of "new","update" or "deleted"
     */
    private String updateStatus = "new";

    /** The type of result. this may be "CE"(coded), TX(Text, alpha-numeric) */
    private String typeResult;

    /**
     * A pair representing the result as text and as code. Even if it's a coded
     * result, it's text is used so that when the code isn't known in receiving
     * system, the text may be used as reference
     */
    private CodedValueXmit result;

    /**
     * @return the typeResult
     */
    public String getTypeResult() {
        return typeResult;
    }

    /**
     * @param typeResult the typeResult to set
     */
    public void setTypeResult(String typeResult) {
        this.typeResult = typeResult;
    }

    public void setResult(CodedValueXmit result) {
        this.result = result;
    }

    public CodedValueXmit getResult() {
        return result;
    }

    public void setUpdateStatus(String updateStatus) {
        this.updateStatus = updateStatus;
    }

    public String getUpdateStatus() {
        return updateStatus;
    }
}
