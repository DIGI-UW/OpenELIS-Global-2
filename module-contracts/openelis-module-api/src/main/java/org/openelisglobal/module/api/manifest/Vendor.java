package org.openelisglobal.module.api.manifest;

/** Who publishes the module, shown in the registry and the admin page. */
public class Vendor {

    private String name;
    private String url;
    private String contact;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }
}
