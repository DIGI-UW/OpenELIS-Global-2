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
package org.openelisglobal.common.provider.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.resources.ResourceLocator;

/**
 * This class will abstract the DataProvider creation. It will read the name of
 * the class file from properties file and create the class
 *
 * @version 1.0
 * @author diane benz bugzilla 2443
 */
public class DataProviderFactory {

    private static class SingletonHelper {
        private static final DataProviderFactory INSTANCE = new DataProviderFactory(); // Instance of this
    }

    // class

    // Properties object that holds data provider mappings
    private Properties dataProviderClassMap = null;

    /** Singleton global access for DataProviderFactory */
    public static DataProviderFactory getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Create an object for the full class name passed in.
     *
     * @param String full class name
     * @return Object Created object
     */
    protected Object createObject(String className) throws LIMSRuntimeException {
        Object object = null;
        try {
            Class classDefinition = Class.forName(className);
            object = classDefinition.newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Unable to create an object for " + className, e);
        }
        return object;
    }

    /**
     * Search for the DataProvider implementation class name in the Data.properties
     * file for the given DataProvider name
     *
     * @param String DataProvider name e.g "NextTestSortOrderDataProvider"
     * @return String Full implementation class e.g
     *         "org.openelisglobal.common.data.provider"
     */
    protected String getDataProviderClassName(String dataProvidername) throws LIMSRuntimeException {
        if (dataProviderClassMap == null) { // Need to load the property
            // object with the class
            // mappings
            ResourceLocator rl = ResourceLocator.getInstance();
            InputStream propertyStream = null;
            // Now load a java.util.Properties object with the properties
            dataProviderClassMap = new Properties();
            try {
                propertyStream = rl.getNamedResourceAsInputStream(ResourceLocator.AJAX_PROPERTIES);

                dataProviderClassMap.load(propertyStream);
            } catch (IOException e) {
                // bugzilla 2154
                LogEvent.logError(e);
                throw new LIMSRuntimeException("Unable to load data provider class mappings.", e);
            } finally {
                if (null != propertyStream) {
                    try {
                        propertyStream.close();
                    } catch (IOException e) {
                        // bugzilla 2154
                        LogEvent.logError(e);
                    }
                }
            }
        }

        String mapping = dataProviderClassMap.getProperty(dataProvidername);
        if (mapping == null) {
            // bugzilla 2154
            // LogEvent.logError(this.getClass().getSimpleName(),
            // "getDataProviderClassName",
            // dataProvidername, e);
            throw new LIMSRuntimeException("getDataProviderClassName - Unable to find mapping for " + dataProvidername);
        }
        return mapping;
    }

    /**
     * Data Provider creation method
     *
     * @param name
     * @return Data Provider object
     */
    public BaseDataProvider getDataProvider(String name) throws LIMSRuntimeException {
        BaseDataProvider provider = null;

        String className = getDataProviderClassName(name);

        provider = (BaseDataProvider) createObject(className);

        return provider;
    }
}
