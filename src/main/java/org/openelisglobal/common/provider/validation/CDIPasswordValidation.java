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
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.common.provider.validation;

public class CDIPasswordValidation implements ILoginPasswordValidation {

    // This is just going to delegate to the HaitiPasswordValidation until they
    // differ
    // i.e. this is just a placeholder
    public boolean passwordValid(String password) {
        return new HaitiPasswordValidation().passwordValid(password);
    }

    public String getInstructions() {
        return new HaitiPasswordValidation().getInstructions();
    }
}
