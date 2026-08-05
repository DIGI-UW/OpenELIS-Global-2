package org.openelisglobal.textmacro.form;

import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.Set;

public class TextMacroAdminForm {
    public String id;
    public String code;
    public String expansionText;
    public Set<String> contexts = new LinkedHashSet<>();
    public boolean active = true;
    public String provenance;
    public String sourceKey;
    public String sourceVersion;
    public Timestamp lastupdated;
}
