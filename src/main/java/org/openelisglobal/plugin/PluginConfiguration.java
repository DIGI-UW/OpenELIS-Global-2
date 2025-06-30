package org.openelisglobal.plugin;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.Servlet;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.io.IOUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.ServletWrappingController;

@Configuration
public class PluginConfiguration implements BeanFactoryAware {

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @PostConstruct
    public void onPostConstruct() {
        File pluginDir = new File(PluginLoader.PLUGIN_ANALYZER);
        loadDirectory(pluginDir);
    }

    private void loadDirectory(File pluginDir) {

        File[] files = pluginDir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith("jar")) {
                    loadPluginServlet(file);
                } else if (file.isDirectory()) {
                    LogEvent.logInfo(this.getClass().getSimpleName(), "loadDirectory",
                            "Checking plugin subfolder: " + file.getName());
                    loadDirectory(file);
                }
            }
        }
    }

    private void loadPluginServlet(File pluginFile) {
        JarFile jar = null;
        try {
            jar = new JarFile(pluginFile);
            if (!PluginLoader.checkJDKVersions(pluginFile.getName(), jar)) {
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

    private boolean loadFromXML(JarFile jar, JarEntry entry) {
        Attribute description = null;
        try {
            URL url = new URL("jar:file:///" + jar.getName() + "!/");
            InputStream input = jar.getInputStream(entry);

            String xml = IOUtils.toString(input, "UTF-8");

            // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", xml);

            Document doc = DocumentHelper.parseText(xml);

            Element versionElement = doc.getRootElement().element(PluginLoader.VERSION);

            if (versionElement == null) {
                LogEvent.logError(this.getClass().getSimpleName(), "loadFromXml", "Missing version number in plugin");
                LogEvent.logInfo(this.getClass().getSimpleName(), "loadFromXml", "Missing version number in plugin");
                return false;
            }
            if (!PluginLoader.SUPPORTED_VERSION.equals(versionElement.getData())) {
                LogEvent.logError(this.getClass().getSimpleName(), "loadFromXml",
                        "Unsupported version number.  Expected " + PluginLoader.SUPPORTED_VERSION + " got "
                                + versionElement.getData());
                LogEvent.logInfo(this.getClass().getSimpleName(), "loadFromXml",
                        "Unsupported version number.  Expected " + PluginLoader.SUPPORTED_VERSION + " got "
                                + versionElement.getData());
                return false;
            }

            Element servlet = doc.getRootElement().element(PluginLoader.SERVLET);

            if (servlet != null) {
                description = servlet.element(PluginLoader.EXTENSION_POINT).element(PluginLoader.DESCRIPTION)
                        .attribute(PluginLoader.VALUE);
                Attribute servletPath = servlet.element(PluginLoader.EXTENSION_POINT).element(PluginLoader.SERVLET_PATH)
                        .attribute(PluginLoader.VALUE);
                Attribute servletName = servlet.element(PluginLoader.EXTENSION_POINT).element(PluginLoader.SERVLET_NAME)
                        .attribute(PluginLoader.VALUE);
                Attribute classPath = servlet.element(PluginLoader.EXTENSION_POINT).element(PluginLoader.EXTENSION)
                        .attribute(PluginLoader.PATH);
                loadActualPlugin(url, servletName.getValue(), servletPath.getValue(), classPath.getValue());
                LogEvent.logInfo(this.getClass().getSimpleName(), "loadFromXML", "Loaded: " + description.getValue());
            }

        } catch (MalformedURLException e) {
            LogEvent.logDebug(e);
            return false;
        } catch (IOException e) {
            LogEvent.logDebug(e);
            return false;
        } catch (DocumentException e) {
            LogEvent.logDebug(e);
            return false;
        }

        return true;
    }

    private void loadActualPlugin(URL url, String servletName, String servletPath, String servletClassPath)
            throws IOException {
        ClassLoader classLoader = null;
        ConfigurableBeanFactory configurableBeanFactory = (ConfigurableBeanFactory) beanFactory;
        try {
            URL[] urls = { url };
            classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
            Class<?> namedClass = classLoader.loadClass(servletClassPath);
            if (Servlet.class.isAssignableFrom(namedClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends Servlet> servletClass = (Class<? extends Servlet>) namedClass;

                if (!configurableBeanFactory.containsBean("SimpleUrlHandlerMapping")) {
                    SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
                    Properties urlProperties = new Properties();
                    urlProperties.put("/plugin" + servletPath, servletName);
                    mapping.setMappings(urlProperties);
                    mapping.setOrder(Integer.MAX_VALUE - 2);
                    configurableBeanFactory.registerSingleton("SimpleUrlHandlerMapping", mapping);
                }
                // configurableBeanFactory.getBean("SimpleUrlHandlerMapping");

                ServletWrappingController controller = new ServletWrappingController();
                controller.setServletClass(servletClass);
                controller.setBeanName(servletName);
                configurableBeanFactory.registerSingleton(servletName, controller);
                try {
                    controller.afterPropertiesSet();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (ClassNotFoundException e) {
            LogEvent.logError("could not load a plugin servlet class with name: " + servletName, e);
        }
        // finally {
        // if (classLoader != null) {
        // classLoader.close();
        // }
        // }
    }
}
