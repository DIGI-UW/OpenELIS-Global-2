package org.openelisglobal.microbiology.form;

/**
 * Server-side worklist request state. The REST controller normalizes request
 * parameters into this form so the same canonical state can be preserved in the
 * browser URL and applied by the service.
 */
public class MicroWorklistQueryForm {

    public String grain = "cultures";
    public String status = "";
    public String workflow = "";
    public String stage = "";
    public String urgency = "";
    public String due = "";
    public String q = "";
    public String sort = "priority";
    public int page = 1;
    public int pageSize = 20;
}
