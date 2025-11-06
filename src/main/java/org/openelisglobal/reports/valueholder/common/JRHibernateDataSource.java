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
package org.openelisglobal.reports.valueholder.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import org.apache.commons.beanutils.PropertyUtils;
import org.openelisglobal.common.log.LogEvent;

/**
 * @author benzd1 bugzilla 2264 bugzilla 2325 - implement JRRewindableDataSource
 *         moveFirst to fix problem with local oc4j (app server hangs)
 */
public class JRHibernateDataSource implements JRRewindableDataSource {

    protected HashMap fieldsToIdxMap = new HashMap();

    protected Iterator iterator;

    protected Object currentValue;

    // bugzilla 2325
    protected Collection records;

    public JRHibernateDataSource(Collection list) {
        this.iterator = list.iterator();
        // bugzilla 2325
        records = list;
    }

    public JRHibernateDataSource(Map list) {
        this.iterator = list.values().iterator();
        // bugzilla 2325
        records = (Collection) list;
    }

    private Object nestedFieldValue(Object object, String field) {
        Object value = null;
        if (field.indexOf("__") > -1) {
            try {
                Method nestedGetter = PropertyUtils.getReadMethod(
                        PropertyUtils.getPropertyDescriptor(object, field.substring(0, field.indexOf("__"))));
                Object nestedObject = nestedGetter.invoke(object, (Object[]) null);
                value = nestedFieldValue(nestedObject, field.substring(field.indexOf("__") + 2, field.length()));
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                // bugzilla 2154
                LogEvent.logError(e);
            }
        } else {
            try {
                Method getter = PropertyUtils.getReadMethod(PropertyUtils.getPropertyDescriptor(object, field));
                value = getter.invoke(object, (Object[]) null);
                if (Collection.class.isAssignableFrom(getter.getReturnType())) {
                    return new JRHibernateDataSource((Collection) value);
                }
                if (Map.class.isAssignableFrom(getter.getReturnType())) {
                    return new JRHibernateDataSource((Map) value);
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                // bugzilla 2154
                LogEvent.logError(e);
            }
        }
        return value;
    }

    public Object getCurrentValue() throws JRException {
        return currentValue;
    }

    // for reuse of datasource in subreports
    public Object getPreviousValue() throws JRException {
        return currentValue;
    }

    @Override
    public Object getFieldValue(JRField field) throws JRException {
        return nestedFieldValue(currentValue, field.getName());
    }

    @Override
    public boolean next() throws JRException {
        currentValue = iterator.hasNext() ? iterator.next() : null;
        return currentValue != null;
    }

    // bugzilla 2325
    @Override
    public void moveFirst() throws JRException {
        // reinitialize the iterator for JRRewindableDataSource
        this.iterator = records.iterator();
    }
}
