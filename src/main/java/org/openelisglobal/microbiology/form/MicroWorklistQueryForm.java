package org.openelisglobal.microbiology.form;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side worklist request state. The REST controller normalizes request
 * parameters into this form so the same canonical state can be preserved in the
 * browser URL and applied by the service.
 */
public class MicroWorklistQueryForm {

    public String grain = "cultures";
    public String status = "";
    public String from = "";
    public String to = "";
    public List<String> specimen = new ArrayList<>();
    public List<String> organism = new ArrayList<>();
    public List<String> origin = new ArrayList<>();
    public List<String> significance = new ArrayList<>();
    public String stage = "";
    public String urgency = "";
    public String due = "";
    public String q = "";
    public String sort = "priority";
    public int page = 1;
    public int pageSize = 20;

    /**
     * Which sample items the caller may read, by their lab units; null when the
     * caller reads every case. Set by the controller, never bound from the request.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public java.util.function.Predicate<String> sampleItemVisible;
}
