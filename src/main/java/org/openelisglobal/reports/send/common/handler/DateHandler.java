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
package org.openelisglobal.reports.send.common.handler;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.exolab.castor.mapping.GeneralizedFieldHandler;
import org.openelisglobal.common.log.LogEvent;

/** The FieldHandler for the Date class */
public class DateHandler extends GeneralizedFieldHandler {

    private static final String FORMAT = "yyyy-MM-dd";

    /** Creates a new MyDateHandler instance */
    public DateHandler() {
        super();
    }

    /**
     * This method is used to convert the value when the getValue method is called.
     * The getValue method will obtain the actual field value from given 'parent'
     * object. This convert method is then invoked with the field's value. The value
     * returned from this method will be the actual value returned by getValue
     * method.
     *
     * @param value the object value to convert after performing a get operation
     * @return the converted value.
     */
    @Override
    public Object convertUponGet(Object value) {
        if (value == null) {
            return null;
        }
        SimpleDateFormat formatter = new SimpleDateFormat(FORMAT);
        Date date = (Date) value;
        return formatter.format(date);
    }

    /**
     * This method is used to convert the value when the setValue method is called.
     * The setValue method will call this method to obtain the converted value. The
     * converted value will then be used as the value to set for the field.
     *
     * @param value the object value to convert before performing a set operation
     * @return the converted value.
     */
    @Override
    public Object convertUponSet(Object value) {
        SimpleDateFormat formatter = new SimpleDateFormat(FORMAT);
        Date date = null;
        try {
            // date = formatter.parse((String)value);
            date = new java.sql.Date(formatter.parse((String) value).getTime());

        } catch (ParseException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new IllegalArgumentException(e.getMessage());
        }
        return date;
    }

    /**
     * Returns the class type for the field that this GeneralizedFieldHandler
     * converts to and from. This should be the type that is used in the object
     * model.
     *
     * @return the class type of of the field
     */
    @Override
    public Class getFieldType() {
        return Date.class;
    }

    /**
     * Creates a new instance of the object described by this field.
     *
     * @param parent The object for which the field is created
     * @return A new instance of the field's value
     * @throws IllegalStateException This field is a simple type and cannot be
     *                               instantiated
     */
    @Override
    public Object newInstance(Object parent) throws IllegalStateException {
        // -- Since it's marked as a string...just return null,
        // -- it's not needed.
        return null;
    }
}
