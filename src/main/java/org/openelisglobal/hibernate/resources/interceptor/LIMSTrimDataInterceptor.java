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
package org.openelisglobal.hibernate.resources.interceptor;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.openelisglobal.audittrail.valueholder.History;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation. bugzilla 1939 (trim changed
 *         data before update/insert) this interceptor is registered in
 *         HibernateUtil (it is a global interceptor)
 *         <p>
 *         originally designed to trim leading and trailing spaces from a string
 *         bugzilla 2109 added trimming of spaces embedded in a string (replace
 *         multiple spaces with 1 space)
 */
public class LIMSTrimDataInterceptor extends EmptyInterceptor {

    private static final long serialVersionUID = 1L;

    private static final String PATTERN_STRING = "[ ]+";
    private static final String REPLACEMENT_STRING = " ";
    private static final Pattern TRIM_PATTERN = Pattern.compile(PATTERN_STRING);

    // for inserts all strings should be trimmed
    public boolean onSave(Object entity, Serializable id, Object[] currentState, String[] propertyNames, Type[] types) {

        // we do not need to be concerned about tables that are not maintained
        // by the User
        // History table is only used to store audit trail
        if (!(entity instanceof History)) {

            for (int i = 0; i < currentState.length; i++) {
                if (currentState[i] instanceof String) {
                    currentState[i] = trimWhiteSpace((String) currentState[i]);
                }
            }
        }
        return super.onSave(entity, id, currentState, propertyNames, types);
    }

    // for updates only if entity isDirty and a particular property of type string
    // has changed should trim occur
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState,
            String[] propertyNames, Type[] types) {

        // loop through properties
        for (int i = 0; i < currentState.length; i++) {
            if (currentState[i] != previousState[i] && currentState[i] instanceof String) {
                currentState[i] = trimWhiteSpace((String) currentState[i]);
            }
        }

        return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
    }

    private String trimWhiteSpace(String origional) {
        origional = origional.trim();

        // bugzilla 2109 now replace multiple spaces with one space
        Matcher matcher = TRIM_PATTERN.matcher(origional);
        return matcher.replaceAll(REPLACEMENT_STRING);
        // end bugzilla 2109

    }
}
