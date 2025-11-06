/** */
package org.openelisglobal.patient.valueholder;

import java.io.Serializable;
import org.openelisglobal.validation.annotations.SafeHtml;

public class AdverseEffect implements Serializable {
    private static final long serialVersionUID = 1L;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String type;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String grade;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }
}
