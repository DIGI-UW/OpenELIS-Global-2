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
package org.openelisglobal.common.provider.reports;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.resources.ResourceLocator;

/**
 * This class will abstract the ReportsProvider creation. It will read the name
 * of the class file from properties file and create the class
 *
 * @version 1.0
 * @author diane benz
 */
public class ReportsProviderFactory {

    private static class SingletonHelper {
        private static final ReportsProviderFactory INSTANCE = new ReportsProviderFactory(); // Instance of this
    }

    // class

    // Properties object that holds reports provider mappings
    private Properties reportsProviderClassMap = null;

    /** Singleton global access for ReportsProviderFactory */
    public static ReportsProviderFactory getInstance() {
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
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Unable to create an object for " + className, e);
        }
        return object;
    }

    /**
     * Search for the ReportsProvider implementation class name in the
     * Reports.properties file for the given ReportsProvider name
     *
     * @param String ReportsProvider name e.g "MycologyWorksheetProvider"
     * @return String Full implementation class e.g
     *         "org.openelisglobal.common.reports.provider"
     */
    protected String getReportsProviderClassName(String reportsProvidername) throws LIMSRuntimeException {
        if (reportsProviderClassMap == null) { // Need to load the property
            // object with the class
            // mappings
            ResourceLocator rl = ResourceLocator.getInstance();
            InputStream propertyStream = null;
            // Now load a java.util.Properties object with the properties
            reportsProviderClassMap = new Properties();
            try {
                propertyStream = rl.getNamedResourceAsInputStream(ResourceLocator.REPORTS_PROPERTIES);

                reportsProviderClassMap.load(propertyStream);
            } catch (IOException e) {
                // bugzilla 2154
                LogEvent.logError(e);
                throw new LIMSRuntimeException("Unable to load reports provider class mappings.", e);
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

        String mapping = reportsProviderClassMap.getProperty(reportsProvidername);
        if (mapping == null) {
            // bugzilla 2154
            LogEvent.logError(this.getClass().getSimpleName(), "getReportsProviderClassName", reportsProvidername);
            throw new LIMSRuntimeException(
                    "getReportsProviderClassName - Unable to find mapping for " + reportsProvidername);
        }
        return mapping;
    }

    /**
     * Reports Provider creation method
     *
     * @param name
     * @return Reports Provider object
     */
    public BaseReportsProvider getReportsProvider(String name) throws LIMSRuntimeException {
        BaseReportsProvider provider = null;

        String className = getReportsProviderClassName(name);

        provider = (BaseReportsProvider) createObject(className);

        return provider;
    }
}
