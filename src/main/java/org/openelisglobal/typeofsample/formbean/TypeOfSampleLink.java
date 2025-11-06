package org.openelisglobal.typeofsample.formbean;

import java.beans.Beans;

public class TypeOfSampleLink extends Beans {

    private String sampleName;
    private String linkName;
    private String linkDescription;
    private String id;

    public TypeOfSampleLink(String id, String sampleName, String linkName, String linkDescription) {
        this.id = id;
        this.sampleName = sampleName;
        this.linkName = linkName;
        this.linkDescription = linkDescription;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getLinkName() {
        return linkName;
    }

    public void setLinkName(String linkName) {
        this.linkName = linkName;
    }

    public String getLinkDescription() {
        return linkDescription;
    }

    public void setLinkDescription(String linkDescription) {
        this.linkDescription = linkDescription;
    }
}
