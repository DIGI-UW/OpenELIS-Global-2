package org.openelisglobal.common.provider.autocomplete;

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
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.resources.ResourceLocator;

/**
 * This class will abstract the AutocompleteProvider creation. It will read the
 * name of the class file from properties file and create the class
 *
 * @version 1.0
 * @author diane benz
 */
public class AutocompleteProviderFactory {

    private static class SingletonHelper {
        private static final AutocompleteProviderFactory INSTANCE = new AutocompleteProviderFactory();
    }

    // class

    // Properties object that holds validation provider mappings
    private Properties validationProviderClassMap = null;

    /** Singleton global access for AutocompleteProviderFactory */
    public static AutocompleteProviderFactory getInstance() {
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
     * Search for the AutocompleteProvider implementation class name in the
     * Autocomplete.properties file for the given AutocompleteProvider name
     *
     * @param String AutocompleteProvider name e.g
     *               "OrganizationLocalAbbreviationAutocompleteProvider"
     * @return String Full implementation class e.g
     *         "org.openelisglobal.common.validation.provider"
     */
    protected String getAutocompleteProviderClassName(String validationProvidername) throws LIMSRuntimeException {
        if (validationProviderClassMap == null) { // Need to load the property
            // object with the class
            // mappings
            ResourceLocator rl = ResourceLocator.getInstance();
            InputStream propertyStream = null;
            // Now load a java.util.Properties object with the properties
            validationProviderClassMap = new Properties();
            try {
                propertyStream = rl.getNamedResourceAsInputStream(ResourceLocator.AJAX_PROPERTIES);

                validationProviderClassMap.load(propertyStream);
            } catch (IOException e) {
                // bugzilla 2154
                LogEvent.logError(e);
                throw new LIMSRuntimeException("Unable to load validation provider class mappings.", e);
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

        String mapping = validationProviderClassMap.getProperty(validationProvidername);
        if (mapping == null) {
            // LogEvent.logError(this.getClass().getSimpleName(),
            // "getAutocompleteProviderClassName", validationProvidername);
            throw new LIMSRuntimeException(
                    "getAutocompleteProviderClassName - Unable to find mapping for " + validationProvidername);
        }
        return mapping;
    }

    /**
     * Autocomplete Provider creation method
     *
     * @param name
     * @return Autocomplete Provider object
     */
    public BaseAutocompleteProvider getAutocompleteProvider(String name) throws LIMSRuntimeException {
        BaseAutocompleteProvider provider = null;

        String className = getAutocompleteProviderClassName(name);

        provider = (BaseAutocompleteProvider) createObject(className);

        return provider;
    }
}
