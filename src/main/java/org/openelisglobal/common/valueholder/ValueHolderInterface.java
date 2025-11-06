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
package org.openelisglobal.common.valueholder;

public interface ValueHolderInterface {

    /**
     * Can be used to have transparent indirection toString instantiate the objects.
     */
    public static boolean shouldToStringInstantiate = false;

    /** PUBLIC: Return the value. */
    public Object getValue();

    /**
     * PUBLIC: Return whether the contents have been read from the database. This is
     * used periodically by the indirection policy to determine whether to trigger
     * the database read.
     */
    public boolean isInstantiated();

    /** PUBLIC: Set the value. */
    public void setValue(Object value);
}
