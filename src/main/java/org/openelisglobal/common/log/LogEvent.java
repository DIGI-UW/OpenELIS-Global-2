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
package org.openelisglobal.common.log;

import org.apache.log4j.Category;
import org.owasp.encoder.Encode;

/**
 * @author Hung Nguyen
 */
public class LogEvent {
    private static final int MAX_STACK_DEPTH = 50;
    private static final int MAX_ERROR_DEPTH = 4;

    /**
     * Write to the log file (type error)
     *
     * @param errorMessage the error message
     * @param throwable    the error to log
     */
    public static void logError(String className, String methodName, String errorMessage) {
        getLog().error(
                "Class: " + className + ", Method: " + methodName + ", Error: " + sanitizeLogMessage(errorMessage));
    }

    /**
     * Write to the log file (type error)
     *
     * @param errorMessage the error message
     * @param throwable    the error to log
     */
    public static void logError(String errorMessage, Throwable throwable) {
        logError(errorMessage, throwable, false);
    }

    /**
     * Write to the log file (type error)
     *
     * @param errorMessage  the error message
     * @param throwable     the error to log
     * @param hideException whether to display only errorMessage
     */
    public static void logError(String errorMessage, Throwable throwable, boolean hideException) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        String className = stackTrace[0].getClassName();
        String methodName = stackTrace[0].getMethodName();
        getLog().error(
                "Class: " + className + ", Method: " + methodName + ", Error: " + sanitizeLogMessage(errorMessage));
        if (!hideException) {
            logError(throwable);
        }
    }

    /**
     * Write to the log file (type error)
     *
     * @param throwable the error to log
     */
    public static void logError(Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("Class: ").append(stackTrace[0].getClassName()).append(", Method: ")
                .append(stackTrace[0].getMethodName()).append(", Line: ").append(stackTrace[0].getLineNumber())
                .append(", Message: ").append(sanitizeLogMessage(throwable.getMessage()));
        if (throwable.getCause() != null) {
            logCause(throwable, throwable.getCause(), errorMessage, 0);
        }
        getLog().error(errorMessage.toString());
        if (getLog().isDebugEnabled()) {
            StringBuilder stackErrorMessage = new StringBuilder();
            for (int i = 0; (i < MAX_STACK_DEPTH) && (i < stackTrace.length); ++i) {
                stackErrorMessage.append(sanitizeLogMessage(stackTrace[i].toString()));
                stackErrorMessage.append(System.lineSeparator());
            }
            logDebugWithoutSanitizing(stackErrorMessage.toString(), throwable);
        }
    }

    private static void logCause(Throwable originalThrowable, Throwable throwable, StringBuilder errorMessage,
            int depth) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        errorMessage.append(System.lineSeparator()).append("Class: ").append(stackTrace[0].getClassName())
                .append(", Method: ").append(stackTrace[0].getMethodName()).append(", Line: ")
                .append(stackTrace[0].getLineNumber()).append(", Sub-Message: ")
                .append(sanitizeLogMessage(throwable.getMessage()));
        if (throwable.getCause() != null && depth < MAX_ERROR_DEPTH) {
            logCause(originalThrowable, throwable.getCause(), errorMessage, ++depth);
        }
    }

    public static void logTrace(String className, String methodName, String debugMessage) {
        getLog().trace(
                "Class: " + className + ", Method: " + methodName + ", Trace: " + sanitizeLogMessage(debugMessage));
    }

    /**
     * Write to the log file (type debug)
     *
     * @param className    the class name
     * @param methodName   the method name
     * @param debugMessage the debug message
     */
    public static void logDebug(String className, String methodName, String debugMessage) {
        getLog().debug(
                "Class: " + className + ", Method: " + methodName + ", Debug: " + sanitizeLogMessage(debugMessage));
    }

    /**
     * Write to the log file (type error)
     *
     * @param errorMessage the error message
     * @param throwable    the error to log
     */
    public static void logDebug(String debugMessage, Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        String className = stackTrace[0].getClassName();
        String methodName = stackTrace[0].getMethodName();

        getLog().debug(
                "Class: " + className + ", Method: " + methodName + ", Error: " + sanitizeLogMessage(debugMessage));
    }

    /**
     * Write to the log file (type error)
     *
     * @param errorMessage the error message
     * @param throwable    the error to log
     */
    private static void logDebugWithoutSanitizing(String debugMessage, Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        String className = stackTrace[0].getClassName();
        String methodName = stackTrace[0].getMethodName();

        getLog().debug("Class: " + className + ", Method: " + methodName + ", Error: " + debugMessage);
    }

    /**
     * Write to the log file (type error)
     *
     * @param throwable the error to log
     */
    public static void logDebug(Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        String className = stackTrace[0].getClassName();
        String methodName = stackTrace[0].getMethodName();

        getLog().debug("Class: " + className + ", Method: " + methodName + ", Error: "
                + sanitizeLogMessage(throwable.getMessage()));
    }

    /**
     * Write to the log file (type info)
     *
     * @param className   the class name
     * @param methodName  the method name
     * @param infoMessage the info message
     */
    public static void logInfo(String className, String methodName, String infoMessage) {
        getLog().info("Class: " + className + ", Method: " + methodName + ", Info: " + sanitizeLogMessage(infoMessage));
    }

    /**
     * Write to the log file (type warning)
     *
     * @param className   the class name
     * @param methodName  the method name
     * @param warnMessage the warning message
     */
    public static void logWarn(String className, String methodName, String warnMessage) {
        getLog().warn(
                "Class: " + className + ", Method: " + methodName + ", Warning:" + sanitizeLogMessage(warnMessage));
    }

    /**
     * Write to the log file (type warning)
     *
     * @param className   the class name
     * @param methodName  the method name
     * @param warnMessage the warning message
     */
    public static void logWarn(Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        StringBuilder warnMessage = new StringBuilder();
        warnMessage.append("Class: ").append(stackTrace[0].getClassName()).append(", Method: ")
                .append(stackTrace[0].getMethodName()).append(", Line: ").append(stackTrace[0].getLineNumber())
                .append(", Message: ").append(sanitizeLogMessage(throwable.getMessage()));
        if (throwable.getCause() != null) {
            logCause(throwable, throwable.getCause(), warnMessage, 0);
        }
        getLog().warn(warnMessage.toString());
    }

    /**
     * Write to the log file (type fatal)
     *
     * @param className   the class name
     * @param methodName  the method name
     * @param warnMessage the fatal message
     */
    public static void logFatal(String className, String methodName, String fatalMessage) {
        getLog().fatal(
                "Class: " + className + ", Method: " + methodName + ", Fatal:" + sanitizeLogMessage(fatalMessage));
    }

    private static Category getLog() {
        return Category.getInstance(LogEvent.class);
    }

    // for preventing log forging
    private static String sanitizeLogMessage(String logMessage) {
        if (logMessage != null) {
            String sanitizedLogMessage = logMessage.replace('\n', '_').replace('\r', '_').replace('\t', '_');
            return Encode.forHtml(sanitizedLogMessage);
        }
        return null;
    }
}
