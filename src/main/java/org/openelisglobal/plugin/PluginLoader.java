/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.plugin;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.commons.io.IOUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.InstantiationException;
import org.openelisglobal.common.exception.LIMSException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;

@Component
public class PluginLoader {
    public static final String PLUGIN_ANALYZER = "/var/lib/openelis-global/plugins/";
    public static final String VERSION = "version";
    public static final String SUPPORTED_VERSION = "1.0";
    public static final String PATH = "path";
    public static final String ANALYZER_IMPORTER = "analyzerImporter";
    public static final String MENU = "menu";
    public static final String PERMISSION = "permission";
    public static final String EXTENSION_POINT = "extension_point";
    public static final String EXTENSION = "extension";
    public static final String DESCRIPTION = "description";
    public static final String VALUE = "value";
    public static final String SERVLET = "servlet";
    public static final String SERVLET_PATH = "servlet-path";
    public static final String SERVLET_NAME = "servlet-name";
    public static final int JDK_VERSION_MAJOR;
    public static final int JDK_VERSION_MINOR;

    private static List<String> currentPlugins;

    static {
        String[] version = System.getProperty("java.version").split("\\.");
        JDK_VERSION_MAJOR = Integer.parseInt(version[0]);
        JDK_VERSION_MINOR = Integer.parseInt(version[1]);
    }

    @PostConstruct
    private void load() {
        File pluginDir = new File(PLUGIN_ANALYZER);

        loadDirectory(pluginDir);

        LogEvent.logInfo(PluginLoader.class.getName(), "load", "Plugins loaded");
    }

    private void loadDirectory(File pluginDir) {
        List<String> pluginList = new ArrayList<>();

        File[] files = pluginDir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith("jar")) {
                    loadPlugin(file);
                    pluginList.add(file.getName());
                } else if (file.isDirectory()) {
                    LogEvent.logInfo(PluginLoader.class.getName(), "loadDirectory",
                            "Checking plugin subfolder: " + file.getName());
                    loadDirectory(file);
                }
            }
        }
        registerPluginNames(pluginList);
    }

    private void loadPlugin(File pluginFile) {
        JarFile jar = null;

        try {
            jar = new JarFile(pluginFile);

            if (!checkJDKVersions(pluginFile.getName(), jar)) {
                return;
            }

            final Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                if (entry.getName().contains(".xml")) {
                    boolean valid = loadFromXML(jar, entry);
                    if (valid) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            LogEvent.logDebug(e);
        } finally {
            try {
                if (jar != null) {
                    jar.close();
                }
            } catch (IOException e) {
                LogEvent.logDebug(e);
            }
        }
    }

    public static boolean checkJDKVersions(String fileName, JarFile jar) throws IOException {
        Manifest manifest = jar.getManifest();
        if (manifest == null) {
            LogEvent.logError(PluginLoader.class.getName(), "checkJDKVersion",
                    "Manifest file not in jar file, unable to check jdk versions");
            LogEvent.logInfo(PluginLoader.class.getName(), "checkJDKVersions",
                    "Manifest file not in jar file, unable to check jdk versions");
            return true;
        }

        String buildJdk = manifest.getMainAttributes().getValue("Build-Jdk");
        if (buildJdk == null) {
            LogEvent.logError(PluginLoader.class.getName(), "checkJDKVersion",
                    "JDK version not found in manifest file, unable to check jdk versions");
            LogEvent.logInfo(PluginLoader.class.getName(), "checkJDKVersions",
                    "JDK version not found in manifest file, unable to check jdk versions");
            return true;
        }

        String[] jarVersion = buildJdk.split("\\.");
        int jarVersionMajor = Integer.parseInt(jarVersion[0]);
        int jarVersionMinor = Integer.parseInt(jarVersion[1]);
        if (jarVersionMajor > JDK_VERSION_MAJOR
                || (jarVersionMajor == JDK_VERSION_MAJOR && jarVersionMinor > JDK_VERSION_MINOR)) {
            LogEvent.logError(PluginLoader.class.getName(), "checkJDKVersion",
                    "The plugin " + fileName + " was compiled with a higher JDK version ("
                            + getVersion(jarVersionMajor, jarVersionMinor) + ") than the runtime JDK ("
                            + getVersion(JDK_VERSION_MAJOR, JDK_VERSION_MINOR) + ")");
            LogEvent.logInfo(PluginLoader.class.getName(), "checkJDKVersions",
                    "The plugin " + fileName + " was compiled with a higher JDK version ("
                            + getVersion(jarVersionMajor, jarVersionMinor) + ") than the runtime JDK ("
                            + getVersion(JDK_VERSION_MAJOR, JDK_VERSION_MINOR) + ")");
            return false;
        }
        return true;
    }

    private static String getVersion(int major, int minor) {
        return major + "." + minor;
    }

    private boolean loadFromXML(JarFile jar, JarEntry entry) {
        Attribute description = null;
        try {
            URL url = new URL("jar:file:///" + jar.getName() + "!/");
            InputStream input = jar.getInputStream(entry);

            String xml = IOUtils.toString(input, "UTF-8");

            // LogEvent.logInfo(PluginLoader.class.getName(), "method unkown", xml);

            Document doc = DocumentHelper.parseText(xml);

            Element versionElement = doc.getRootElement().element(VERSION);

            if (versionElement == null) {
                LogEvent.logError(PluginLoader.class.getName(), "loadFromXml", "Missing version number in plugin");
                return false;
            }
            if (!SUPPORTED_VERSION.equals(versionElement.getData())) {
                LogEvent.logError(PluginLoader.class.getName(), "loadFromXml", "Unsupported version number.  Expected "
                        + SUPPORTED_VERSION + " got " + versionElement.getData());
                return false;
            }

            Element analyzerImporter = doc.getRootElement().element(ANALYZER_IMPORTER);

            if (analyzerImporter != null) {
                description = analyzerImporter.element(EXTENSION_POINT).element(DESCRIPTION).attribute(VALUE);
                Attribute path = analyzerImporter.element(EXTENSION_POINT).element(EXTENSION).attribute(PATH);
                loadActualPlugin(url, path.getValue());
                LogEvent.logInfo(PluginLoader.class.getName(), "loadFromXml", "Loaded: " + description.getValue());
            }

            Element menu = doc.getRootElement().element(MENU);

            if (menu != null) {
                description = menu.element(EXTENSION_POINT).element(DESCRIPTION).attribute(VALUE);
                Attribute path = menu.element(EXTENSION_POINT).element(EXTENSION).attribute(PATH);
                loadActualPlugin(url, path.getValue());
                LogEvent.logInfo(PluginLoader.class.getName(), "loadFromXml", "Loaded: " + description.getValue());
            }

            Element permissions = doc.getRootElement().element(PERMISSION);

            if (permissions != null) {
                description = permissions.element(EXTENSION_POINT).element(DESCRIPTION).attribute(VALUE);
                Attribute path = permissions.element(EXTENSION_POINT).element(EXTENSION).attribute(PATH);
                loadActualPlugin(url, path.getValue());
                LogEvent.logInfo(PluginLoader.class.getName(), "loadFromXml", "Loaded: " + description.getValue());
            }

        } catch (IOException | DocumentException | LIMSException e) {
            if (description != null) {
                LogEvent.logError("Failed Loading: " + description.getValue(), e);
            } else {
                LogEvent.logError("Failed Loading: " + jar.getName(), e);
            }
            return false;
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private void loadActualPlugin(URL url, String classPath) throws LIMSException {
        try {
            URL[] urls = { url };
            ClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());

            Class<APlugin> aClass = (Class<APlugin>) classLoader.loadClass(classPath);
            APlugin instance = aClass.newInstance();
            instance.connect();
        } catch (ClassNotFoundException e) {
            LogEvent.logDebug(e);
            throw new LIMSException("See previous stack trace");
        } catch (InstantiationException e) {
            LogEvent.logDebug(e);
            throw new LIMSException("See previous stack trace");
        } catch (IllegalAccessException e) {
            LogEvent.logDebug(e);
            throw new LIMSException("See previous stack trace");
        } catch (java.lang.InstantiationException e) {
            LogEvent.logDebug(e);
            throw new LIMSException("See previous stack trace");
        }
    }

    public static List<String> getCurrentPlugins() {
        return currentPlugins;
    }

    private void registerPluginNames(List<String> listOfPlugins) {
        if (currentPlugins == null) {
            currentPlugins = listOfPlugins;
        }
    }
}
