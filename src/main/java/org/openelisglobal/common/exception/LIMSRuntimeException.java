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
package org.openelisglobal.common.exception;

/**
 * Base exception used by the LIMS. Unless special handling can be done with an
 * exception, errors should throw this exception. In the UI portion of the
 * application, this base exception can be caught and an error page displayed.
 *
 * <p>
 * Specialized exceptions should extend this class.
 */
public class LIMSRuntimeException extends RuntimeException {

    /**
     * Creates a new LIMSException with a detailed message
     *
     * @param String the detailed message
     */
    public LIMSRuntimeException(String pMessage) {
        super(pMessage);
    }

    /**
     * Creates a new LIMSException wrapping another exception including a detailed
     * message.
     *
     * @param String    the detailed message
     * @param Throwable the wrapped exception
     */
    public LIMSRuntimeException(String pMessage, Throwable e) {
        super(pMessage, e);
    }

    /**
     * Creates a new LIMSException wrapping another exception
     *
     * @param Throwable the wrapped exception
     */
    public LIMSRuntimeException(Throwable e) {
        super(e);
    }
}
