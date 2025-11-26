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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.reports.action.implementation.reportBeans;

import java.math.BigDecimal;

/**
 * Data bean for freezer temperature/humidity readings in reports. Used by
 * FreezerDailyLogReport and related report implementations.
 */
public class FreezerReadingReportData {

    private String freezerId;
    private String freezerName;
    private String location;
    private String timestamp;
    private BigDecimal temperature;
    private BigDecimal humidity;
    private String temperatureFormatted;
    private String humidityFormatted;
    private String status;
    private String statusSeverity;
    private String minThreshold;
    private String maxThreshold;
    private String notes;

    public FreezerReadingReportData() {
    }

    public String getFreezerId() {
        return freezerId;
    }

    public void setFreezerId(String freezerId) {
        this.freezerId = freezerId;
    }

    public String getFreezerName() {
        return freezerName;
    }

    public void setFreezerName(String freezerName) {
        this.freezerName = freezerName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public BigDecimal getHumidity() {
        return humidity;
    }

    public void setHumidity(BigDecimal humidity) {
        this.humidity = humidity;
    }

    public String getTemperatureFormatted() {
        return temperatureFormatted;
    }

    public void setTemperatureFormatted(String temperatureFormatted) {
        this.temperatureFormatted = temperatureFormatted;
    }

    public String getHumidityFormatted() {
        return humidityFormatted;
    }

    public void setHumidityFormatted(String humidityFormatted) {
        this.humidityFormatted = humidityFormatted;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusSeverity() {
        return statusSeverity;
    }

    public void setStatusSeverity(String statusSeverity) {
        this.statusSeverity = statusSeverity;
    }

    public String getMinThreshold() {
        return minThreshold;
    }

    public void setMinThreshold(String minThreshold) {
        this.minThreshold = minThreshold;
    }

    public String getMaxThreshold() {
        return maxThreshold;
    }

    public void setMaxThreshold(String maxThreshold) {
        this.maxThreshold = maxThreshold;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
