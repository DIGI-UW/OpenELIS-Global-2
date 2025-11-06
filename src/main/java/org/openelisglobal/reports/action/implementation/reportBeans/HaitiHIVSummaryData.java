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

public class HaitiHIVSummaryData {
    private String testName;
    private int positive;
    private int negative;
    private int indeterminate;
    private int pending;
    private int total;
    private double positivePer = 0.0;
    private double negativePer = 0.0;
    private double indeterminatePer = 0.0;
    private double pendingPer = 0.0;

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public int getPositive() {
        return positive;
    }

    public void setPositive(int positive) {
        this.positive = positive;
    }

    public int getNegative() {
        return negative;
    }

    public void setNegative(int negative) {
        this.negative = negative;
    }

    public int getIndeterminate() {
        return indeterminate;
    }

    public void setIndeterminate(int indeterminate) {
        this.indeterminate = indeterminate;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public double getPositivePer() {
        return positivePer;
    }

    public void setPositivePer(double positivePer) {
        this.positivePer = positivePer;
    }

    public double getNegativePer() {
        return negativePer;
    }

    public void setNegativePer(double negativePer) {
        this.negativePer = negativePer;
    }

    public double getIndeterminatePer() {
        return indeterminatePer;
    }

    public void setIndeterminatePer(double indeterminatePer) {
        this.indeterminatePer = indeterminatePer;
    }

    public int getPending() {
        return pending;
    }

    public void setPending(int pending) {
        this.pending = pending;
    }

    public double getPendingPer() {
        return pendingPer;
    }

    public void setPendingPer(double pendingPer) {
        this.pendingPer = pendingPer;
    }
}
