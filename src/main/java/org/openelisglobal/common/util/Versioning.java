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
package org.openelisglobal.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.DatabaseChangeLogService;
import org.openelisglobal.common.valueholder.DatabaseChangeLog;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.stereotype.Component;

@Component
public class Versioning {

    private final String PROPERTY_FILE = "build.properties";
    private String releaseNumber = " ";

    public Versioning() {
        InputStream propertyStream = null;
        Properties properties = null;

        try {
            propertyStream = this.getClass().getClassLoader().getResourceAsStream(PROPERTY_FILE);
            properties = new Properties();
            properties.load(propertyStream);
        } catch (IOException e) {
            LogEvent.logError(e);
        } finally {
            if (null != propertyStream) {
                try {
                    propertyStream.close();
                } catch (IOException e) {
                    LogEvent.logError(e);
                }
            }
        }
        if (properties != null) {
            releaseNumber = properties.getProperty("project.version", " ");
        }
    }

    public String getDatabaseVersion() {
        DatabaseChangeLogService databaseChangeLogService = SpringContext.getBean(DatabaseChangeLogService.class);
        DatabaseChangeLog changeLog = databaseChangeLogService.getLastExecutedChange();

        if (changeLog != null) {
            return changeLog.getAuthor() + "/" + changeLog.getId() + "/" + changeLog.getFileName();
        }

        return "";
    }

    public String getReleaseNumber() {
        return releaseNumber;
    }
}
