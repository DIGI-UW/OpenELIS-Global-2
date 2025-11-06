package org.openelisglobal.common.provider.query;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.resources.ResourceLocator;

/**
 * This class will abstract the ValidationProvider creation. It will read the
 * name of the class file from properties file and create the class
 *
 * @version 1.0
 * @author diane benz
 */
public class QueryProviderFactory {

    private static class SingletonHelper {
        private static final QueryProviderFactory INSTANCE = new QueryProviderFactory(); // Instance of this
    }

    // class

    // Properties object that holds validation provider mappings
    private Properties queryProviderClassMap = null;

    /** Singleton global access for ValidationProviderFactory */
    public static QueryProviderFactory getInstance() {
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
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Unable to create an object for " + className, e);
        }
        return object;
    }

    /**
     * Search for the ValidationProvider implementation class name in the
     * Validation.properties file for the given ValidationProvider name
     *
     * @param String ValidationProvider name e.g
     *               "OrganizationLocalAbbreviationValidationProvider"
     * @return String Full implementation class e.g
     *         "org.openelisglobal.common.validation.provider"
     */
    protected String getQueryProviderClassName(String queryProvidername) throws LIMSRuntimeException {
        if (queryProviderClassMap == null) { // Need to load the property
            // object with the class
            // mappings
            ResourceLocator rl = ResourceLocator.getInstance();
            InputStream propertyStream = null;
            // Now load a java.util.Properties object with the properties
            queryProviderClassMap = new Properties();
            try {
                propertyStream = rl.getNamedResourceAsInputStream(ResourceLocator.AJAX_PROPERTIES);

                queryProviderClassMap.load(propertyStream);
            } catch (IOException e) {
                LogEvent.logError(e);
                throw new LIMSRuntimeException("Unable to load validation provider class mappings.", e);
            } finally {
                if (null != propertyStream) {
                    try {
                        propertyStream.close();
                    } catch (IOException e) {
                        LogEvent.logError(e.getMessage(), e);
                    }
                }
            }
        }

        String mapping = queryProviderClassMap.getProperty(queryProvidername);
        if (mapping == null) {
            LogEvent.logError(this.getClass().getSimpleName(), "getQueryProviderClassName",
                    "getQueryProviderClassName - Unable to find mapping for " + queryProvidername);
            throw new LIMSRuntimeException(
                    "getQueryProviderClassName - Unable to find mapping for " + queryProvidername);
        }
        return mapping;
    }

    public BaseQueryProvider getQueryProvider(String name) throws LIMSRuntimeException {

        BaseQueryProvider provider = null;

        String className = getQueryProviderClassName(name);

        provider = (BaseQueryProvider) createObject(className);

        return provider;
    }
}
