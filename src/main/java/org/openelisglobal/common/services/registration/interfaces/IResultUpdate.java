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
package org.openelisglobal.common.services.registration.interfaces;

import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.services.IResultSaveService;

public interface IResultUpdate {
    /**
     * Will be called after all core database updates have been done but before the
     * changes have been committed
     *
     * @throws LIMSRuntimeException If thrown all transactions will be rolled back
     */
    public void transactionalUpdate(IResultSaveService resultService) throws LIMSRuntimeException;

    /**
     * Will be called after the transaction has been committed. If the transaction
     * has been rolled back this will not be called
     */
    public void postTransactionalCommitUpdate(IResultSaveService resultService);
}
