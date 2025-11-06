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

import java.io.Serializable;
import org.openelisglobal.common.log.LogEvent;

/**
 * <b>Purpose</b>: Act as a place holder for a variable that required a value
 * holder interface. This class should be used to initialze an objects
 * attributes that are using indirection is their mappings.
 */
public class ValueHolder implements ValueHolderInterface, Cloneable, Serializable {

    /** Stores the wrapped object. */
    protected Object value;

    /** PUBLIC: Initialize the holder. */
    public ValueHolder() {
        super();
    }

    /** PUBLIC: Initialize the holder with an object. */
    public ValueHolder(Object value) {
        this.value = value;
    }

    /** INTERNAL: */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // bugzilla 2154
            LogEvent.logError(e);
        }

        return null;
    }

    /** PUBLIC: Return the wrapped object. */
    @Override
    public synchronized Object getValue() {
        return value;
    }

    /**
     * PUBLIC: Return a boolean indicating whether the wrapped object has been set
     * or not.
     */
    @Override
    public boolean isInstantiated() {
        // Always return true since we consider
        // null to be a valid wrapped object.
        return true;
    }

    /** PUBLIC: Set the wrapped object. */
    @Override
    public void setValue(Object value) {
        this.value = value;
    }

    /** INTERNAL: */
    @Override
    public String toString() {
        if (getValue() == null) {
            return "{" + null + "}";
        }
        return "{" + getValue().toString() + "}";
    }
}
